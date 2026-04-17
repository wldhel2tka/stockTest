<template>
  <div class="min-vh-100 bg-light">
    <!-- 네비게이션 -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div class="container-fluid px-4">
        <span class="navbar-brand fw-bold fs-5">
          <i class="bi bi-graph-up-arrow me-2"></i>StockTest
        </span>
        <div class="d-flex align-items-center gap-3">
          <router-link to="/home" class="btn btn-sm btn-outline-light">
            <i class="bi bi-search me-1"></i>주식 검색
          </router-link>
          <router-link to="/portfolio" class="btn btn-sm btn-outline-light">
            <i class="bi bi-briefcase me-1"></i>포트폴리오
          </router-link>
          <router-link to="/recommend" class="btn btn-sm btn-light text-primary fw-semibold">
            <i class="bi bi-star me-1"></i>추천 종목
          </router-link>
          <span class="text-white small border-start border-light ps-3">
            <i class="bi bi-person-circle me-1"></i>{{ user?.name }} 님
          </span>
          <button class="btn btn-outline-light btn-sm" @click="handleLogout">
            <i class="bi bi-box-arrow-right me-1"></i>로그아웃
          </button>
        </div>
      </div>
    </nav>

    <div class="container-fluid px-4 mt-4">
      <!-- 헤더 -->
      <div class="d-flex align-items-center justify-content-between mb-3">
        <h5 class="fw-bold mb-0"><i class="bi bi-star-fill text-warning me-2"></i>추천 종목</h5>
        <div class="d-flex align-items-center gap-2">
          <!-- 시장 선택 -->
          <div class="btn-group btn-group-sm">
            <button class="btn" :class="market === 'J' ? 'btn-primary' : 'btn-outline-secondary'" @click="setMarket('J')">KOSPI</button>
            <button class="btn" :class="market === 'Q' ? 'btn-primary' : 'btn-outline-secondary'" @click="setMarket('Q')">KOSDAQ</button>
          </div>
          <button class="btn btn-outline-secondary btn-sm" @click="loadData" :disabled="loading">
            <i class="bi bi-arrow-clockwise me-1" :class="{ 'spin': loading }"></i>새로고침
          </button>
        </div>
      </div>

      <!-- 탭 -->
      <ul class="nav nav-tabs mb-3">
        <li class="nav-item">
          <button class="nav-link" :class="{ active: tab === 'volume' }" @click="setTab('volume')">
            <i class="bi bi-bar-chart me-1"></i>거래량 상위
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link" :class="{ active: tab === 'gainers' }" @click="setTab('gainers')">
            <i class="bi bi-arrow-up-circle me-1 text-danger"></i>상승률 상위
          </button>
        </li>
        <li class="nav-item">
          <button class="nav-link" :class="{ active: tab === 'losers' }" @click="setTab('losers')">
            <i class="bi bi-arrow-down-circle me-1 text-primary"></i>하락률 상위
          </button>
        </li>
      </ul>

      <!-- 로딩 -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
        <div class="text-muted mt-2 small">데이터 불러오는 중...</div>
      </div>

      <!-- 에러 -->
      <div v-else-if="error" class="alert alert-warning">
        <i class="bi bi-exclamation-triangle me-1"></i>{{ error }}
      </div>

      <!-- 테이블 -->
      <div v-else class="card border-0 shadow-sm">
        <div class="card-body p-0">
          <div class="table-responsive">
            <table class="table table-hover mb-0 small align-middle">
              <thead class="table-light">
                <tr>
                  <th class="ps-3 text-center" style="width:50px">순위</th>
                  <th>종목명</th>
                  <th class="text-end">현재가</th>
                  <th class="text-end">전일 대비</th>
                  <th class="text-end">등락률</th>
                  <th class="text-end">거래량</th>
                  <th class="text-end pe-3">거래대금</th>
                  <th class="text-center" style="width:90px">매수</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!stocks.length">
                  <td colspan="8" class="text-center text-muted py-4">
                    <i class="bi bi-inbox display-6 opacity-25 d-block mb-2"></i>데이터가 없습니다.
                  </td>
                </tr>
                <tr v-for="s in stocks" :key="s.code">
                  <td class="ps-3 text-center fw-bold text-muted">{{ s.rank }}</td>
                  <td>
                    <strong>{{ s.name }}</strong>
                    <span class="text-muted ms-1" style="font-size:0.75rem">{{ s.code }}</span>
                  </td>
                  <td class="text-end fw-semibold">{{ s.currentPrice.toLocaleString() }}</td>
                  <td class="text-end" :class="s.up ? 'text-danger' : (s.change < 0 ? 'text-primary' : 'text-muted')">
                    {{ s.up ? '+' : '' }}{{ s.change.toLocaleString() }}
                  </td>
                  <td class="text-end fw-bold" :class="s.up ? 'text-danger' : (s.changeRate < 0 ? 'text-primary' : 'text-muted')">
                    {{ s.changeRate >= 0 ? '+' : '' }}{{ s.changeRate.toFixed(2) }}%
                  </td>
                  <td class="text-end text-muted">{{ s.volume.toLocaleString() }}</td>
                  <td class="text-end pe-3 text-muted">{{ formatAmount(s.tradingValue) }}</td>
                  <td class="text-center">
                    <button class="btn btn-danger btn-sm py-0 px-2" @click="openBuy(s)">매수</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- 매수 모달 -->
    <Teleport to="body">
      <div v-if="buyModal"
           class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
           style="background:rgba(0,0,0,0.5);z-index:1055"
           @click.self="buyModal = null">
        <div class="card shadow-lg" style="width:340px">
          <div class="card-header bg-danger text-white fw-bold py-2">
            <i class="bi bi-cart-plus me-1"></i>매수 — {{ buyModal.name }}
          </div>
          <div class="card-body">
            <div class="mb-2 d-flex justify-content-between small text-muted">
              <span>현재가</span>
              <strong class="text-dark">{{ buyModal.currentPrice.toLocaleString() }}원</strong>
            </div>
            <div class="mb-3 d-flex justify-content-between small text-muted">
              <span>가용 잔고</span>
              <strong class="text-dark">{{ balance.toLocaleString() }}원</strong>
            </div>
            <div class="input-group input-group-sm mb-2">
              <button class="btn btn-outline-secondary" @click="buyQty = Math.max(1, buyQty - 1)">−</button>
              <input type="number" class="form-control text-center" v-model.number="buyQty" min="1" />
              <button class="btn btn-outline-secondary" @click="buyQty++">+</button>
            </div>
            <div class="d-flex justify-content-between small mb-1">
              <span class="text-muted">주문금액</span>
              <strong>{{ (buyModal.currentPrice * buyQty).toLocaleString() }}원</strong>
            </div>
            <div class="d-flex justify-content-between small">
              <span class="text-muted">매수 후 잔고</span>
              <strong :class="balance - buyModal.currentPrice * buyQty < 0 ? 'text-danger' : ''">
                {{ (balance - buyModal.currentPrice * buyQty).toLocaleString() }}원
              </strong>
            </div>
          </div>
          <div class="card-footer d-flex gap-2 py-2">
            <button class="btn btn-secondary flex-fill btn-sm" @click="buyModal = null">취소</button>
            <button class="btn btn-danger flex-fill btn-sm"
                    :disabled="tradeLoading || buyQty < 1 || balance < buyModal.currentPrice * buyQty"
                    @click="executeBuy">
              <span v-if="tradeLoading" class="spinner-border spinner-border-sm me-1"></span>
              매수
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getTopVolume, getGainers, getLosers, getAccount, buyStock } from '../api/index.js'

const router = useRouter()
const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

const tab = ref('volume')
const market = ref('J')
const stocks = ref([])
const loading = ref(false)
const error = ref('')
const balance = ref(0)

const buyModal = ref(null)
const buyQty = ref(1)
const tradeLoading = ref(false)

async function loadBalance() {
  if (!user.value?.id) return
  try {
    const res = await getAccount(user.value.id)
    balance.value = res.data.balance
  } catch (e) {
    // ignore
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    let res
    if (tab.value === 'volume')  res = await getTopVolume(market.value)
    else if (tab.value === 'gainers') res = await getGainers(market.value)
    else res = await getLosers(market.value)
    stocks.value = res.data
  } catch (e) {
    error.value = e.response?.data?.message || '데이터를 불러오지 못했습니다.'
    stocks.value = []
  } finally {
    loading.value = false
  }
}

function setTab(t) {
  tab.value = t
  loadData()
}

function setMarket(m) {
  market.value = m
  loadData()
}

function openBuy(stock) {
  buyModal.value = stock
  buyQty.value = 1
}

async function executeBuy() {
  if (!user.value?.id || !buyModal.value) return
  tradeLoading.value = true
  try {
    await buyStock(user.value.id, buyModal.value.code, buyQty.value)
    buyModal.value = null
    await loadBalance()
    alert('매수 완료!')
  } catch (e) {
    alert(e.response?.data?.message || '매수 실패')
  } finally {
    tradeLoading.value = false
  }
}

function formatAmount(v) {
  if (v >= 1_000_000_000_000) return (v / 1_000_000_000_000).toFixed(1) + '조'
  if (v >= 100_000_000) return (v / 100_000_000).toFixed(0) + '억'
  if (v >= 10_000) return (v / 10_000).toFixed(0) + '만'
  return v.toLocaleString()
}

function handleLogout() {
  sessionStorage.removeItem('user')
  router.push('/')
}

onMounted(() => {
  loadBalance()
  loadData()
})
</script>

<style scoped>
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
