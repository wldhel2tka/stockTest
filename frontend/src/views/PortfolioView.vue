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
          <router-link to="/portfolio" class="btn btn-sm btn-light text-primary fw-semibold">
            <i class="bi bi-briefcase me-1"></i>포트폴리오
          </router-link>
          <router-link to="/youtube" class="btn btn-sm btn-outline-light">
            <i class="bi bi-youtube me-1"></i>유튜버 분석
          </router-link>
          <router-link to="/news" class="btn btn-sm btn-outline-light">
            <i class="bi bi-newspaper me-1"></i>뉴스 분석
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

      <!-- 로딩 -->
      <div v-if="loading" class="text-center py-5">
        <div class="spinner-border text-primary"></div>
        <div class="text-muted mt-2">포트폴리오 불러오는 중...</div>
      </div>

      <template v-else>
        <!-- 계좌 요약 카드 -->
        <div class="row g-3 mb-4">
          <div class="col-md-3">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body text-center py-3">
                <div class="text-muted small mb-1"><i class="bi bi-wallet2 me-1"></i>가용 잔고</div>
                <div class="fw-bold fs-5">{{ fmtMoney(portfolio?.balance) }}원</div>
              </div>
            </div>
          </div>
          <div class="col-md-3">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body text-center py-3">
                <div class="text-muted small mb-1"><i class="bi bi-bar-chart-fill me-1"></i>주식 평가금액</div>
                <div class="fw-bold fs-5">{{ fmtMoney(portfolio?.totalCurrentValue) }}원</div>
              </div>
            </div>
          </div>
          <div class="col-md-3">
            <div class="card border-0 shadow-sm h-100">
              <div class="card-body text-center py-3">
                <div class="text-muted small mb-1"><i class="bi bi-piggy-bank me-1"></i>총 자산</div>
                <div class="fw-bold fs-5">{{ fmtMoney(portfolio?.totalAssets) }}원</div>
                <div class="small" :class="pnlClass">
                  {{ portfolio?.totalPnl >= 0 ? '+' : '' }}{{ fmtMoney(portfolio?.totalPnl) }}원
                  ({{ portfolio?.totalPnlRate >= 0 ? '+' : '' }}{{ portfolio?.totalPnlRate.toFixed(2) }}%)
                </div>
              </div>
            </div>
          </div>
          <div class="col-md-3 d-flex align-items-center justify-content-center">
            <div class="d-flex flex-column gap-2 w-100">
              <button class="btn btn-outline-secondary btn-sm" @click="loadPortfolio">
                <i class="bi bi-arrow-clockwise me-1"></i>새로고침
              </button>
              <button class="btn btn-outline-danger btn-sm" @click="confirmReset">
                <i class="bi bi-trash3 me-1"></i>계좌 초기화
              </button>
            </div>
          </div>
        </div>

        <!-- 보유 종목 -->
        <div class="card border-0 shadow-sm mb-4">
          <div class="card-header bg-white fw-semibold py-2 px-3">
            <i class="bi bi-briefcase me-1 text-primary"></i>보유 종목
            <span class="badge bg-primary ms-1">{{ portfolio?.holdings.length || 0 }}</span>
          </div>
          <div class="card-body p-0">
            <div v-if="!portfolio?.holdings.length" class="text-center text-muted py-4">
              <i class="bi bi-inbox display-6 opacity-25"></i>
              <p class="mt-2 mb-0 small">보유 중인 종목이 없습니다.</p>
            </div>
            <div v-else class="table-responsive">
              <table class="table table-hover mb-0 small">
                <thead class="table-light">
                  <tr>
                    <th class="ps-3">종목명</th>
                    <th class="text-end">수량</th>
                    <th class="text-end">평균단가</th>
                    <th class="text-end">현재가</th>
                    <th class="text-end">평가금액</th>
                    <th class="text-end pe-3">평가손익 (수익률)</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="h in portfolio.holdings" :key="h.stockCode">
                    <td class="ps-3">
                      <strong>{{ h.stockName }}</strong>
                      <span class="text-muted ms-1" style="font-size: 0.75rem;">{{ h.stockCode }}</span>
                    </td>
                    <td class="text-end">{{ h.quantity }}주</td>
                    <td class="text-end">{{ fmtMoney(h.avgPrice) }}</td>
                    <td class="text-end">{{ fmtMoney(h.currentPrice) }}</td>
                    <td class="text-end">{{ fmtMoney(h.currentValue) }}</td>
                    <td class="text-end pe-3" :class="h.pnl >= 0 ? 'text-danger' : 'text-primary'">
                      <strong>{{ h.pnl >= 0 ? '+' : '' }}{{ fmtMoney(h.pnl) }}원</strong>
                      <span class="ms-1">({{ h.pnlRate >= 0 ? '+' : '' }}{{ h.pnlRate.toFixed(2) }}%)</span>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <!-- 거래내역 -->
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white fw-semibold py-2 px-3">
            <i class="bi bi-clock-history me-1 text-secondary"></i>거래 내역
            <span class="text-muted small ms-1">(최근 100건)</span>
          </div>
          <div class="card-body p-0">
            <div v-if="!transactions.length" class="text-center text-muted py-4">
              <i class="bi bi-inbox display-6 opacity-25"></i>
              <p class="mt-2 mb-0 small">거래 내역이 없습니다.</p>
            </div>
            <div v-else class="table-responsive">
              <table class="table table-hover mb-0 small">
                <thead class="table-light">
                  <tr>
                    <th class="ps-3">일시</th>
                    <th class="text-center">구분</th>
                    <th>종목명</th>
                    <th class="text-end">수량</th>
                    <th class="text-end">단가</th>
                    <th class="text-end pe-3">거래금액</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="tx in transactions" :key="tx.id">
                    <td class="ps-3 text-muted">{{ formatDateTime(tx.createdAt) }}</td>
                    <td class="text-center">
                      <span class="badge" :class="tx.type === 'BUY' ? 'bg-danger' : 'bg-primary'">
                        {{ tx.type === 'BUY' ? '매수' : '매도' }}
                      </span>
                    </td>
                    <td>
                      <strong>{{ tx.stockName }}</strong>
                      <span class="text-muted ms-1" style="font-size: 0.75rem;">{{ tx.stockCode }}</span>
                    </td>
                    <td class="text-end">{{ tx.quantity }}주</td>
                    <td class="text-end">{{ fmtMoney(tx.price) }}</td>
                    <td class="text-end pe-3 fw-semibold" :class="tx.type === 'BUY' ? 'text-danger' : 'text-primary'">
                      {{ tx.type === 'BUY' ? '-' : '+' }}{{ fmtMoney(tx.totalAmount) }}원
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 초기화 확인 모달 -->
    <Teleport to="body">
      <div v-if="showResetConfirm"
           class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
           style="background: rgba(0,0,0,0.5); z-index: 1055;"
           @click.self="showResetConfirm = false">
        <div class="card shadow-lg" style="width: 320px;">
          <div class="card-header bg-danger text-white fw-bold py-2">
            <i class="bi bi-exclamation-triangle me-1"></i>계좌 초기화
          </div>
          <div class="card-body">
            <p class="mb-1">모든 보유 종목과 거래내역이 삭제되고 잔고가 <strong>10,000,000원</strong>으로 초기화됩니다.</p>
            <p class="text-danger small mb-0">이 작업은 되돌릴 수 없습니다.</p>
          </div>
          <div class="card-footer d-flex gap-2 py-2">
            <button class="btn btn-secondary flex-fill btn-sm" @click="showResetConfirm = false">취소</button>
            <button class="btn btn-danger flex-fill btn-sm" :disabled="resetLoading" @click="executeReset">
              <span v-if="resetLoading" class="spinner-border spinner-border-sm me-1"></span>
              초기화
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPortfolio, getTransactions, resetAccount } from '../api/index.js'

const router = useRouter()
const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

const portfolio = ref(null)
const transactions = ref([])
const loading = ref(true)
const showResetConfirm = ref(false)
const resetLoading = ref(false)

const pnlClass = computed(() => {
  if (!portfolio.value) return ''
  return portfolio.value.totalPnl >= 0 ? 'text-danger' : 'text-primary'
})

async function loadPortfolio() {
  if (!user.value?.id) return
  loading.value = true
  try {
    const [portRes, txRes] = await Promise.all([
      getPortfolio(user.value.id),
      getTransactions(user.value.id)
    ])
    portfolio.value = portRes.data
    transactions.value = txRes.data
  } catch (e) {
    console.error('포트폴리오 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

function confirmReset() {
  showResetConfirm.value = true
}

async function executeReset() {
  if (!user.value?.id) return
  resetLoading.value = true
  try {
    await resetAccount(user.value.id)
    showResetConfirm.value = false
    await loadPortfolio()
  } catch (e) {
    console.error('초기화 실패', e)
  } finally {
    resetLoading.value = false
  }
}

function fmtMoney(v) {
  return Math.round(v || 0).toLocaleString('ko-KR')
}

function formatDateTime(dt) {
  if (!dt) return ''
  return dt.replace('T', ' ').substring(0, 16)
}

function handleLogout() {
  sessionStorage.removeItem('user')
  router.push('/')
}

onMounted(loadPortfolio)
</script>
