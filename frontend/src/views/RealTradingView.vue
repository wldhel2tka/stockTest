<template>
  <AppLayout>
    <div class="p-4">
      <div class="d-flex align-items-center gap-2 mb-4">
        <i class="bi bi-bank fs-4 text-primary"></i>
        <h4 class="mb-0 fw-bold">실계좌 주문</h4>
        <span class="badge bg-danger ms-1">실전투자</span>
      </div>

      <div class="row g-4">
        <!-- 왼쪽: 잔고 + 주문 -->
        <div class="col-lg-4">

          <!-- 잔고 카드 -->
          <div class="card shadow-sm mb-3">
            <div class="card-header d-flex justify-content-between align-items-center">
              <span class="fw-semibold"><i class="bi bi-wallet2 me-1"></i>계좌 잔고</span>
              <button class="btn btn-outline-secondary btn-sm" @click="loadBalance" :disabled="balanceLoading">
                <span v-if="balanceLoading" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-arrow-clockwise"></i>
              </button>
            </div>
            <div class="card-body">
              <div v-if="balanceError" class="text-danger small">{{ balanceError }}</div>
              <div v-else-if="balance">
                <div class="mb-2">
                  <div class="text-muted small">주문 가능 현금</div>
                  <div class="fs-5 fw-bold text-primary">{{ fmt(balance.orderableAmt) }}원</div>
                </div>
                <div class="row g-2 text-center">
                  <div class="col-6">
                    <div class="bg-light rounded p-2">
                      <div class="text-muted" style="font-size:0.75rem">주식 평가금액</div>
                      <div class="fw-semibold small">{{ fmt(balance.stockValue) }}원</div>
                    </div>
                  </div>
                  <div class="col-6">
                    <div class="bg-light rounded p-2">
                      <div class="text-muted" style="font-size:0.75rem">총 자산</div>
                      <div class="fw-semibold small">{{ fmt(balance.totalAssets) }}원</div>
                    </div>
                  </div>
                  <div class="col-6">
                    <div class="bg-light rounded p-2">
                      <div class="text-muted" style="font-size:0.75rem">평가손익</div>
                      <div class="fw-semibold small" :class="balance.pnl >= 0 ? 'text-danger' : 'text-primary'">
                        {{ balance.pnl >= 0 ? '+' : '' }}{{ fmt(balance.pnl) }}원
                      </div>
                    </div>
                  </div>
                  <div class="col-6">
                    <div class="bg-light rounded p-2">
                      <div class="text-muted" style="font-size:0.75rem">수익률</div>
                      <div class="fw-semibold small" :class="balance.pnlRate >= 0 ? 'text-danger' : 'text-primary'">
                        {{ balance.pnlRate >= 0 ? '+' : '' }}{{ balance.pnlRate?.toFixed(2) }}%
                      </div>
                    </div>
                  </div>
                </div>
              </div>
              <div v-else class="text-muted small text-center py-2">잔고를 불러오는 중...</div>
            </div>
          </div>

          <!-- 주문 폼 카드 -->
          <div class="card shadow-sm">
            <div class="card-header fw-semibold">
              <i class="bi bi-send me-1"></i>주문 입력
            </div>
            <div class="card-body">
              <!-- 종목 검색 -->
              <div class="mb-3 position-relative">
                <label class="form-label small fw-semibold">종목</label>
                <input
                  type="text"
                  class="form-control form-control-sm"
                  v-model="searchQuery"
                  placeholder="종목명 또는 코드"
                  @input="onSearchInput"
                  @keydown.down.prevent="acIdx = Math.min(acIdx + 1, acList.length - 1)"
                  @keydown.up.prevent="acIdx = Math.max(acIdx - 1, 0)"
                  @keydown.enter.prevent="selectStock(acList[acIdx])"
                  @blur="setTimeout(() => acList = [], 150)"
                />
                <ul v-if="acList.length" class="list-group position-absolute w-100 shadow-sm" style="z-index:200;top:100%">
                  <li
                    v-for="(s, i) in acList" :key="s.code"
                    class="list-group-item list-group-item-action py-1 px-2 small"
                    :class="{ active: i === acIdx }"
                    @mousedown.prevent="selectStock(s)"
                  >
                    <span class="fw-semibold">{{ s.name }}</span>
                    <span class="text-muted ms-2">{{ s.code }}</span>
                  </li>
                </ul>
              </div>

              <!-- 선택된 종목 현재가 -->
              <div v-if="selectedStock" class="alert alert-light py-2 px-3 mb-3 small">
                <span class="fw-bold">{{ selectedStock.name }}</span>
                <span class="ms-2 text-muted">{{ selectedStock.code }}</span>
                <span class="ms-2 fw-bold" :class="priceColor">{{ fmt(currentPrice) }}원</span>
                <span v-if="changeRate !== null" class="ms-1 small" :class="priceColor">
                  ({{ changeRate >= 0 ? '+' : '' }}{{ changeRate?.toFixed(2) }}%)
                </span>
              </div>

              <!-- 매수/매도 탭 -->
              <div class="btn-group w-100 mb-3">
                <button class="btn btn-sm" :class="side === 'BUY' ? 'btn-danger' : 'btn-outline-danger'" @click="side = 'BUY'">매수</button>
                <button class="btn btn-sm" :class="side === 'SELL' ? 'btn-primary' : 'btn-outline-primary'" @click="side = 'SELL'">매도</button>
              </div>

              <!-- 주문 유형 -->
              <div class="mb-3">
                <label class="form-label small fw-semibold">주문 유형</label>
                <select class="form-select form-select-sm" v-model="orderType">
                  <option value="MARKET">시장가</option>
                  <option value="LIMIT">지정가</option>
                </select>
              </div>

              <!-- 지정가 입력 -->
              <div v-if="orderType === 'LIMIT'" class="mb-3">
                <label class="form-label small fw-semibold">주문 단가</label>
                <div class="input-group input-group-sm">
                  <input type="number" class="form-control" v-model.number="limitPrice" min="1" placeholder="0" />
                  <span class="input-group-text">원</span>
                </div>
              </div>

              <!-- 수량 -->
              <div class="mb-3">
                <label class="form-label small fw-semibold">수량</label>
                <div class="input-group input-group-sm">
                  <button class="btn btn-outline-secondary" @click="quantity = Math.max(1, quantity - 1)">−</button>
                  <input type="number" class="form-control text-center" v-model.number="quantity" min="1" />
                  <button class="btn btn-outline-secondary" @click="quantity++">+</button>
                </div>
                <div class="text-muted mt-1" style="font-size:0.75rem">
                  예상 금액: <strong>{{ fmt(estimatedAmt) }}원</strong>
                </div>
              </div>

              <!-- 경고 배너 -->
              <div class="alert alert-warning py-2 px-3 small mb-3">
                <i class="bi bi-exclamation-triangle me-1"></i>
                실제 계좌에서 <strong>실제 주문</strong>이 체결됩니다.
              </div>

              <div v-if="orderError" class="alert alert-danger py-2 px-3 small mb-2">{{ orderError }}</div>
              <div v-if="orderSuccess" class="alert alert-success py-2 px-3 small mb-2">{{ orderSuccess }}</div>

              <button
                class="btn w-100 btn-sm fw-bold"
                :class="side === 'BUY' ? 'btn-danger' : 'btn-primary'"
                :disabled="!selectedStock || quantity < 1 || ordering"
                @click="showConfirm = true"
              >
                <span v-if="ordering" class="spinner-border spinner-border-sm me-1"></span>
                {{ side === 'BUY' ? '매수 주문' : '매도 주문' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 오른쪽: 보유 종목 + 주문 내역 -->
        <div class="col-lg-8">

          <!-- 보유 종목 -->
          <div class="card shadow-sm mb-3">
            <div class="card-header d-flex justify-content-between align-items-center">
              <span class="fw-semibold"><i class="bi bi-pie-chart me-1"></i>보유 종목</span>
            </div>
            <div class="card-body p-0">
              <div v-if="balanceLoading" class="text-center py-4">
                <span class="spinner-border spinner-border-sm text-primary"></span>
              </div>
              <div v-else-if="holdings.length === 0" class="text-muted text-center py-4 small">보유 종목이 없습니다.</div>
              <div v-else class="table-responsive">
                <table class="table table-sm table-hover mb-0 small">
                  <thead class="table-light">
                    <tr>
                      <th>종목명</th>
                      <th class="text-end">보유수량</th>
                      <th class="text-end">평균단가</th>
                      <th class="text-end">현재가</th>
                      <th class="text-end">평가금액</th>
                      <th class="text-end">평가손익</th>
                      <th class="text-end">수익률</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="h in holdings" :key="h.code" style="cursor:pointer" @click="quickSelect(h)">
                      <td>
                        <div class="fw-semibold">{{ h.name }}</div>
                        <div class="text-muted" style="font-size:0.7rem">{{ h.code }}</div>
                      </td>
                      <td class="text-end">{{ h.qty }}주</td>
                      <td class="text-end">{{ fmt(h.avgPrice) }}원</td>
                      <td class="text-end">{{ fmt(h.currentPrice) }}원</td>
                      <td class="text-end">{{ fmt(h.currentValue) }}원</td>
                      <td class="text-end" :class="h.pnl >= 0 ? 'text-danger' : 'text-primary'">
                        {{ h.pnl >= 0 ? '+' : '' }}{{ fmt(h.pnl) }}원
                      </td>
                      <td class="text-end" :class="h.pnlRate >= 0 ? 'text-danger' : 'text-primary'">
                        {{ h.pnlRate >= 0 ? '+' : '' }}{{ h.pnlRate?.toFixed(2) }}%
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <!-- 주문 체결 내역 -->
          <div class="card shadow-sm">
            <div class="card-header d-flex justify-content-between align-items-center">
              <span class="fw-semibold"><i class="bi bi-clock-history me-1"></i>주문 체결 내역</span>
              <button class="btn btn-outline-secondary btn-sm" @click="loadOrders" :disabled="ordersLoading">
                <span v-if="ordersLoading" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-arrow-clockwise"></i>
              </button>
            </div>
            <div class="card-body p-0">
              <div v-if="ordersLoading" class="text-center py-4">
                <span class="spinner-border spinner-border-sm text-primary"></span>
              </div>
              <div v-else-if="ordersError" class="text-danger text-center py-4 small">{{ ordersError }}</div>
              <div v-else-if="orders.length === 0" class="text-muted text-center py-4 small">체결 내역이 없습니다.</div>
              <div v-else class="table-responsive">
                <table class="table table-sm table-hover mb-0 small">
                  <thead class="table-light">
                    <tr>
                      <th>주문일시</th>
                      <th>구분</th>
                      <th>종목명</th>
                      <th class="text-end">주문수량</th>
                      <th class="text-end">체결수량</th>
                      <th class="text-end">체결단가</th>
                      <th class="text-end">주문금액</th>
                      <th>상태</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(o, i) in orders" :key="i">
                      <td class="text-nowrap">{{ formatDate(o.ord_dt, o.ord_tmd) }}</td>
                      <td>
                        <span class="badge" :class="o.sll_buy_dvsn_cd === '02' ? 'bg-danger' : 'bg-primary'">
                          {{ o.sll_buy_dvsn_cd === '02' ? '매수' : '매도' }}
                        </span>
                      </td>
                      <td>
                        <div class="fw-semibold">{{ o.prdt_name }}</div>
                        <div class="text-muted" style="font-size:0.7rem">{{ o.pdno }}</div>
                      </td>
                      <td class="text-end">{{ Number(o.ord_qty).toLocaleString() }}주</td>
                      <td class="text-end">{{ Number(o.tot_ccld_qty).toLocaleString() }}주</td>
                      <td class="text-end">{{ fmt(Number(o.avg_prvs)) }}원</td>
                      <td class="text-end">{{ fmt(Number(o.ord_unpr) * Number(o.ord_qty)) }}원</td>
                      <td>
                        <span class="badge" :class="o.ord_qty === o.tot_ccld_qty ? 'bg-success' : o.tot_ccld_qty === '0' ? 'bg-secondary' : 'bg-warning text-dark'">
                          {{ o.ord_qty === o.tot_ccld_qty ? '전량체결' : o.tot_ccld_qty === '0' ? '미체결' : '부분체결' }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 주문 확인 모달 -->
    <div v-if="showConfirm" class="modal d-block" tabindex="-1" style="background:rgba(0,0,0,0.5)">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">주문 확인</h5>
            <button class="btn-close" @click="showConfirm = false"></button>
          </div>
          <div class="modal-body">
            <table class="table table-sm mb-0">
              <tbody>
                <tr><th class="text-muted fw-normal">종목</th><td class="fw-bold">{{ selectedStock?.name }} ({{ selectedStock?.code }})</td></tr>
                <tr><th class="text-muted fw-normal">구분</th>
                  <td><span class="fw-bold" :class="side === 'BUY' ? 'text-danger' : 'text-primary'">{{ side === 'BUY' ? '매수' : '매도' }}</span></td>
                </tr>
                <tr><th class="text-muted fw-normal">주문유형</th><td>{{ orderType === 'MARKET' ? '시장가' : '지정가' }}</td></tr>
                <tr v-if="orderType === 'LIMIT'"><th class="text-muted fw-normal">주문단가</th><td>{{ fmt(limitPrice) }}원</td></tr>
                <tr><th class="text-muted fw-normal">수량</th><td>{{ quantity }}주</td></tr>
                <tr><th class="text-muted fw-normal">예상금액</th><td class="fw-bold">{{ fmt(estimatedAmt) }}원</td></tr>
              </tbody>
            </table>
            <div class="alert alert-danger mt-3 mb-0 small py-2">
              <i class="bi bi-exclamation-triangle me-1"></i>
              실제 계좌에서 실제 주문이 체결됩니다. 확인 후 진행하세요.
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="showConfirm = false">취소</button>
            <button class="btn fw-bold" :class="side === 'BUY' ? 'btn-danger' : 'btn-primary'" @click="submitOrder" :disabled="ordering">
              <span v-if="ordering" class="spinner-border spinner-border-sm me-1"></span>
              {{ side === 'BUY' ? '매수 확인' : '매도 확인' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import AppLayout from '../components/AppLayout.vue'
import { searchStocks, getStockPrice, realOrder, getRealBalance, getRealOrders } from '../api/index.js'

// 잔고
const balance = ref(null)
const holdings = ref([])
const balanceLoading = ref(false)
const balanceError = ref('')

// 주문 내역
const orders = ref([])
const ordersLoading = ref(false)
const ordersError = ref('')

// 종목 검색
const searchQuery = ref('')
const acList = ref([])
const acIdx = ref(0)
const selectedStock = ref(null)
const currentPrice = ref(0)
const changeRate = ref(null)

// 주문 폼
const side = ref('BUY')
const orderType = ref('MARKET')
const limitPrice = ref(0)
const quantity = ref(1)
const ordering = ref(false)
const orderError = ref('')
const orderSuccess = ref('')
const showConfirm = ref(false)

const estimatedAmt = computed(() => {
  const p = orderType.value === 'LIMIT' ? limitPrice.value : currentPrice.value
  return p * quantity.value
})

const priceColor = computed(() => changeRate.value >= 0 ? 'text-danger' : 'text-primary')

const fmt = (v) => Number(v || 0).toLocaleString()

function formatDate(dt, tmd) {
  if (!dt) return '-'
  const d = `${dt.slice(0,4)}-${dt.slice(4,6)}-${dt.slice(6,8)}`
  const t = tmd ? `${tmd.slice(0,2)}:${tmd.slice(2,4)}:${tmd.slice(4,6)}` : ''
  return t ? `${d} ${t}` : d
}

async function loadBalance() {
  balanceLoading.value = true
  balanceError.value = ''
  try {
    const res = await getRealBalance()
    const body = res.data
    // output2: 계좌 요약, output1: 보유 종목 목록
    const summary = body.output2?.[0] || {}
    balance.value = {
      orderableAmt: Number(summary.ord_psbl_cash || 0),
      stockValue:   Number(summary.scts_evlu_amt || 0),
      totalAssets:  Number(summary.tot_evlu_amt || 0),
      pnl:          Number(summary.evlu_pfls_smtl_amt || 0),
      pnlRate:      Number(summary.asst_icdc_erng_rt || 0),
    }
    holdings.value = (body.output1 || [])
      .filter(h => Number(h.hldg_qty) > 0)
      .map(h => ({
        code:         h.pdno,
        name:         h.prdt_name,
        qty:          Number(h.hldg_qty),
        avgPrice:     Number(h.pchs_avg_pric),
        currentPrice: Number(h.prpr),
        currentValue: Number(h.evlu_amt),
        pnl:          Number(h.evlu_pfls_amt),
        pnlRate:      Number(h.evlu_erng_rt),
      }))
  } catch (e) {
    balanceError.value = e.response?.data?.message || '잔고 조회 실패'
  } finally {
    balanceLoading.value = false
  }
}

async function loadOrders() {
  ordersLoading.value = true
  ordersError.value = ''
  try {
    const res = await getRealOrders()
    orders.value = res.data || []
  } catch (e) {
    ordersError.value = e.response?.data?.message || '주문 내역 조회 실패'
  } finally {
    ordersLoading.value = false
  }
}

let searchTimer = null
async function onSearchInput() {
  selectedStock.value = null
  acList.value = []
  if (!searchQuery.value.trim()) return
  clearTimeout(searchTimer)
  searchTimer = setTimeout(async () => {
    try {
      const res = await searchStocks(searchQuery.value)
      acList.value = res.data?.slice(0, 8) || []
      acIdx.value = 0
    } catch { acList.value = [] }
  }, 250)
}

async function selectStock(s) {
  if (!s) return
  selectedStock.value = s
  searchQuery.value = `${s.name} (${s.code})`
  acList.value = []
  try {
    const res = await getStockPrice(s.code)
    currentPrice.value = res.data?.currentPrice || 0
    changeRate.value = res.data?.changeRate ?? null
    if (orderType.value === 'LIMIT') limitPrice.value = currentPrice.value
  } catch { currentPrice.value = 0 }
}

function quickSelect(h) {
  selectStock({ code: h.code, name: h.name })
  side.value = 'SELL'
  quantity.value = 1
}

async function submitOrder() {
  ordering.value = true
  orderError.value = ''
  orderSuccess.value = ''
  try {
    await realOrder(
      selectedStock.value.code,
      quantity.value,
      side.value,
      orderType.value,
      orderType.value === 'LIMIT' ? limitPrice.value : 0
    )
    orderSuccess.value = `${side.value === 'BUY' ? '매수' : '매도'} 주문이 접수되었습니다.`
    showConfirm.value = false
    quantity.value = 1
    await loadBalance()
    await loadOrders()
  } catch (e) {
    orderError.value = e.response?.data?.message || '주문 실패'
    showConfirm.value = false
  } finally {
    ordering.value = false
  }
}

onMounted(() => {
  loadBalance()
  loadOrders()
})
</script>
