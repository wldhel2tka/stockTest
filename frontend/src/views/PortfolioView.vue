<template>
  <div>
    <div class="container-fluid px-4 mt-4">

      <!-- 자동매매 패널 -->
      <div class="card border-0 shadow-sm mb-4" :class="autoEnabled ? 'border-success border-2' : ''">
        <div class="card-header bg-white d-flex align-items-center justify-content-between py-2 px-3">
          <span class="fw-semibold">
            <i class="bi bi-robot me-1 text-success"></i>자동매매
            <span class="badge ms-1" :class="autoEnabled ? 'bg-success' : 'bg-secondary'">
              {{ autoEnabled ? 'ON' : 'OFF' }}
            </span>
          </span>
          <div class="d-flex align-items-center gap-2">
            <span v-if="autoLastRun" class="text-muted" style="font-size:0.75rem">
              마지막 실행: {{ autoLastRun }}
            </span>
            <button class="btn btn-outline-secondary btn-sm" @click="runNow" :disabled="runNowLoading">
              <span v-if="runNowLoading" class="spinner-border spinner-border-sm"></span>
              <span v-else><i class="bi bi-play-fill me-1"></i>지금 실행</span>
            </button>
            <div class="form-check form-switch mb-0">
              <input class="form-check-input" type="checkbox" :checked="autoEnabled" @change="toggleAuto" style="width:2.5rem;height:1.25rem;cursor:pointer">
            </div>
          </div>
        </div>
        <div class="card-body py-3 px-3">
          <div class="row g-3 align-items-end">
            <div class="col-md-3">
              <label class="form-label small fw-semibold text-success">익절 기준</label>
              <div class="input-group input-group-sm">
                <input type="number" class="form-control" v-model.number="cfg.takeProfitPct" min="1" max="100" step="0.5" />
                <span class="input-group-text">%</span>
              </div>
              <div class="text-muted mt-1" style="font-size:0.72rem">매수가 대비 수익 도달 시 전량 매도</div>
            </div>
            <div class="col-md-3">
              <label class="form-label small fw-semibold text-primary">손절 기준</label>
              <div class="input-group input-group-sm">
                <input type="number" class="form-control" v-model.number="cfg.stopLossPct" min="1" max="100" step="0.5" />
                <span class="input-group-text">%</span>
              </div>
              <div class="text-muted mt-1" style="font-size:0.72rem">매수가 대비 손실 도달 시 전량 매도</div>
            </div>
            <div class="col-md-3">
              <label class="form-label small fw-semibold">1회 매수금액</label>
              <div class="input-group input-group-sm">
                <input type="number" class="form-control" v-model.number="cfg.buyAmount" min="100000" step="100000" />
                <span class="input-group-text">원</span>
              </div>
              <div class="text-muted mt-1" style="font-size:0.72rem">BUY 신호 종목 1건당 매수금액</div>
            </div>
            <div class="col-md-3">
              <button class="btn btn-dark btn-sm w-100" @click="saveConfig" :disabled="configSaving">
                <span v-if="configSaving" class="spinner-border spinner-border-sm me-1"></span>
                <i v-else class="bi bi-check-lg me-1"></i>설정 저장
              </button>
              <div class="text-muted mt-1" style="font-size:0.72rem">장 시간(09:00~15:30)에만 동작, 1분 간격</div>
            </div>
          </div>
        </div>

        <!-- 자동매매 로그 -->
        <div v-if="autoLogs.length" class="card-footer bg-light p-0">
          <div class="px-3 py-2 d-flex justify-content-between align-items-center">
            <span class="small fw-semibold text-muted">자동매매 로그</span>
            <button class="btn btn-link btn-sm p-0 text-muted" @click="showLogs = !showLogs">
              {{ showLogs ? '접기' : '펼치기' }}
            </button>
          </div>
          <div v-if="showLogs" class="table-responsive">
            <table class="table table-sm mb-0" style="font-size:0.78rem">
              <thead class="table-light">
                <tr>
                  <th class="ps-3">시간</th>
                  <th>구분</th>
                  <th>종목</th>
                  <th class="text-end">단가</th>
                  <th class="text-end">수량</th>
                  <th class="text-end pe-3">금액</th>
                  <th>사유</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(l, i) in autoLogs" :key="i">
                  <td class="ps-3 text-muted text-nowrap">{{ formatLogTime(l.time) }}</td>
                  <td>
                    <span class="badge"
                      :class="l.type === 'BUY' ? 'bg-danger' : l.type === 'SELL_PROFIT' ? 'bg-success' : 'bg-primary'">
                      {{ l.type === 'BUY' ? '자동매수' : l.type === 'SELL_PROFIT' ? '익절' : '손절' }}
                    </span>
                  </td>
                  <td><strong>{{ l.stockName }}</strong> <span class="text-muted">{{ l.stockCode }}</span></td>
                  <td class="text-end">{{ Number(l.price).toLocaleString() }}원</td>
                  <td class="text-end">{{ l.quantity }}주</td>
                  <td class="text-end pe-3">{{ Number(l.amount).toLocaleString() }}원</td>
                  <td class="text-muted" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">{{ l.reason }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

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
                  ({{ portfolio?.totalPnlRate >= 0 ? '+' : '' }}{{ portfolio?.totalPnlRate?.toFixed(2) }}%)
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
                      <span class="ms-1">({{ h.pnlRate >= 0 ? '+' : '' }}{{ h.pnlRate?.toFixed(2) }}%)</span>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  getPortfolio, getTransactions, resetAccount,
  getAutoTradingStatus, toggleAutoTrading, setAutoTradingConfig,
  getAutoTradingLogs, runAutoTradingNow
} from '../api/index.js'

const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

// 포트폴리오
const portfolio = ref(null)
const transactions = ref([])
const loading = ref(true)
const showResetConfirm = ref(false)
const resetLoading = ref(false)

// 자동매매
const autoEnabled = ref(false)
const autoLastRun = ref(null)
const autoLogs = ref([])
const showLogs = ref(true)
const configSaving = ref(false)
const runNowLoading = ref(false)
const cfg = ref({ takeProfitPct: 10, stopLossPct: 5, buyAmount: 1000000 })

let pollTimer = null

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

async function loadAutoStatus() {
  try {
    const [statusRes, logsRes] = await Promise.all([
      getAutoTradingStatus(),
      getAutoTradingLogs()
    ])
    const s = statusRes.data
    autoEnabled.value = s.enabled
    autoLastRun.value = s.lastRunTime ? s.lastRunTime.replace('T', ' ').substring(0, 16) : null
    cfg.value.takeProfitPct = s.takeProfitPct
    cfg.value.stopLossPct   = s.stopLossPct
    cfg.value.buyAmount     = s.buyAmount
    autoLogs.value = logsRes.data || []
  } catch (e) {
    console.error('자동매매 상태 조회 실패', e)
  }
}

async function toggleAuto(e) {
  const enabled = e.target.checked
  try {
    await toggleAutoTrading(enabled)
    autoEnabled.value = enabled
  } catch (e) {
    console.error('자동매매 토글 실패', e)
  }
}

async function saveConfig() {
  configSaving.value = true
  try {
    await setAutoTradingConfig(cfg.value)
  } finally {
    configSaving.value = false
  }
}

async function runNow() {
  runNowLoading.value = true
  try {
    await runAutoTradingNow()
    await Promise.all([loadPortfolio(), loadAutoStatus()])
  } finally {
    runNowLoading.value = false
  }
}

function confirmReset() { showResetConfirm.value = true }

async function executeReset() {
  if (!user.value?.id) return
  resetLoading.value = true
  try {
    await resetAccount(user.value.id)
    showResetConfirm.value = false
    await loadPortfolio()
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

function formatLogTime(t) {
  if (!t) return ''
  return String(t).replace('T', ' ').substring(0, 16)
}

onMounted(async () => {
  await Promise.all([loadPortfolio(), loadAutoStatus()])
  // 30초마다 로그·상태 갱신
  pollTimer = setInterval(loadAutoStatus, 30_000)
})

onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>
