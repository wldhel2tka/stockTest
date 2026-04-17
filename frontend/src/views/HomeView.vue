<template>
  <div class="min-vh-100 bg-light">
    <!-- 네비게이션 -->
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div class="container-fluid px-4">
        <span class="navbar-brand fw-bold fs-5">
          <i class="bi bi-graph-up-arrow me-2"></i>StockTest
        </span>
        <div class="d-flex align-items-center gap-3">
          <span class="text-white small">
            <i class="bi bi-person-circle me-1"></i>{{ user?.name }} 님
          </span>
          <button class="btn btn-outline-light btn-sm" @click="handleLogout">
            <i class="bi bi-box-arrow-right me-1"></i>로그아웃
          </button>
        </div>
      </div>
    </nav>

    <div class="container-fluid px-4 mt-4">
      <!-- 검색 영역 -->
      <div class="row justify-content-center mb-4">
        <div class="col-md-6 col-lg-5">
          <div class="position-relative">
            <div class="input-group shadow-sm">
              <span class="input-group-text bg-white border-end-0">
                <i class="bi bi-search text-muted"></i>
              </span>
              <input
                v-model="searchQuery"
                @input="onSearchInput"
                @keydown.down.prevent="moveSuggestion(1)"
                @keydown.up.prevent="moveSuggestion(-1)"
                @keydown.enter.prevent="onEnter"
                @blur="hideSuggestions"
                type="text"
                class="form-control border-start-0 border-end-0 ps-0"
                placeholder="종목명 또는 종목코드 (예: 삼성전자, 005930)"
              />
              <button class="btn btn-primary" @click="onEnter">
                검색
              </button>
            </div>

            <!-- 자동완성 드롭다운 -->
            <ul
              v-if="showSuggestions && suggestions.length > 0"
              class="list-group position-absolute w-100 shadow border-0"
              style="z-index: 1000; top: 100%; margin-top: 2px;"
            >
              <li
                v-for="(s, idx) in suggestions"
                :key="s.code"
                @mousedown.prevent="selectStock(s)"
                class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-2 px-3"
                :class="{ active: idx === highlightIdx }"
                style="cursor: pointer;"
              >
                <span class="fw-semibold">{{ s.name }}</span>
                <div class="d-flex gap-2 align-items-center">
                  <span class="badge rounded-pill" :class="s.market === 'KOSPI' ? 'bg-primary' : 'bg-success'">
                    {{ s.market }}
                  </span>
                  <code class="small text-muted">{{ s.code }}</code>
                </div>
              </li>
            </ul>
          </div>
          <div v-if="searchError" class="text-danger small mt-1">
            <i class="bi bi-exclamation-circle me-1"></i>{{ searchError }}
          </div>
        </div>
      </div>

      <!-- 주가 정보 카드 -->
      <div v-if="stockPrice" class="row mb-3">
        <div class="col-12">
          <div class="card border-0 shadow-sm">
            <div class="card-body py-3">
              <div class="row align-items-center">
                <!-- 종목명 + 현재가 -->
                <div class="col-md-6">
                  <div class="d-flex align-items-baseline gap-2 mb-1">
                    <h5 class="mb-0 fw-bold">{{ stockPrice.name }}</h5>
                    <small class="text-muted">{{ stockPrice.code }}</small>
                    <span class="badge" :class="stockPrice.up ? 'bg-danger' : changeZero ? 'bg-secondary' : 'bg-primary'">
                      {{ stockPrice.up ? '▲ 상승' : changeZero ? '— 보합' : '▼ 하락' }}
                    </span>
                  </div>
                  <div class="d-flex align-items-baseline gap-3">
                    <span class="display-6 fw-bold lh-1" :class="priceColorClass">
                      {{ stockPrice.currentPrice.toLocaleString() }}
                      <small class="fs-6 fw-normal">원</small>
                    </span>
                    <span class="fs-5 fw-semibold" :class="priceColorClass">
                      {{ stockPrice.change >= 0 ? '+' : '' }}{{ stockPrice.change.toLocaleString() }}
                      ({{ stockPrice.changeRate >= 0 ? '+' : '' }}{{ stockPrice.changeRate.toFixed(2) }}%)
                    </span>
                  </div>
                </div>
                <!-- 시/고/저 + 거래량 -->
                <div class="col-md-6 mt-3 mt-md-0">
                  <div class="row g-2 text-center">
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">시가</div>
                        <div class="fw-semibold small">{{ stockPrice.openPrice.toLocaleString() }}</div>
                      </div>
                    </div>
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">고가</div>
                        <div class="fw-semibold small text-danger">{{ stockPrice.highPrice.toLocaleString() }}</div>
                      </div>
                    </div>
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">저가</div>
                        <div class="fw-semibold small text-primary">{{ stockPrice.lowPrice.toLocaleString() }}</div>
                      </div>
                    </div>
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">거래량</div>
                        <div class="fw-semibold small">{{ formatVolume(stockPrice.volume) }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 차트 영역 -->
      <div v-if="selectedStock" class="row">
        <div class="col-12">
          <div class="card border-0 shadow-sm">
            <div class="card-header bg-white d-flex justify-content-between align-items-center py-2 px-3">
              <span class="fw-semibold text-secondary small">
                <i class="bi bi-bar-chart-fill me-1"></i>캔들 차트
              </span>
              <div class="btn-group btn-group-sm">
                <button
                  v-for="p in periodOptions"
                  :key="p.key"
                  @click="changePeriod(p)"
                  :class="['btn', selectedPeriod.key === p.key ? 'btn-primary' : 'btn-outline-secondary']"
                >
                  {{ p.label }}
                </button>
              </div>
            </div>
            <div class="card-body p-3">
              <div v-if="chartLoading" class="text-center py-5">
                <div class="spinner-border text-primary mb-2"></div>
                <div class="text-muted small">차트 데이터 로딩 중...</div>
              </div>
              <div v-show="!chartLoading" ref="chartContainer" style="width: 100%; height: 460px;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 빈 상태 -->
      <div v-if="!selectedStock" class="text-center py-5 text-muted">
        <i class="bi bi-bar-chart-line display-1 opacity-25"></i>
        <p class="mt-3 fs-5">종목을 검색하여 주가 차트를 확인하세요</p>
        <p class="small">KOSPI / KOSDAQ 주요 종목 지원</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { searchStocks, getStockPrice, getStockChart } from '../api/index.js'
import { createChart, CrosshairMode } from 'lightweight-charts'

const router = useRouter()
const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

// 검색
const searchQuery = ref('')
const suggestions = ref([])
const showSuggestions = ref(false)
const highlightIdx = ref(-1)
const searchError = ref('')
let searchTimer = null

// 선택된 종목
const selectedStock = ref(null)
const stockPrice = ref(null)

// 차트
const chartContainer = ref(null)
const chartLoading = ref(false)
let chart = null
let candleSeries = null
let volumeSeries = null

// 기간 옵션
const periodOptions = [
  { key: '1M',  label: '1개월', period: 'D', days: 30 },
  { key: '3M',  label: '3개월', period: 'D', days: 90 },
  { key: '6M',  label: '6개월', period: 'D', days: 180 },
  { key: '1Y',  label: '1년',   period: 'W', days: 365 },
  { key: '3Y',  label: '3년',   period: 'M', days: 1095 },
]
const selectedPeriod = ref(periodOptions[1]) // 기본 3개월

// Computed
const changeZero = computed(() => stockPrice.value && stockPrice.value.change === 0)
const priceColorClass = computed(() => {
  if (!stockPrice.value) return ''
  if (stockPrice.value.up) return 'text-danger'
  if (stockPrice.value.change < 0) return 'text-primary'
  return 'text-dark'
})

// 검색 input 처리
function onSearchInput() {
  highlightIdx.value = -1
  searchError.value = ''
  if (searchTimer) clearTimeout(searchTimer)

  const q = searchQuery.value.trim()
  if (!q) {
    suggestions.value = []
    showSuggestions.value = false
    return
  }

  searchTimer = setTimeout(async () => {
    try {
      const res = await searchStocks(q)
      suggestions.value = res.data
      showSuggestions.value = true
    } catch {
      suggestions.value = []
    }
  }, 250)
}

function moveSuggestion(dir) {
  if (!showSuggestions.value || suggestions.value.length === 0) return
  highlightIdx.value = Math.max(-1, Math.min(suggestions.value.length - 1, highlightIdx.value + dir))
}

function onEnter() {
  if (highlightIdx.value >= 0 && suggestions.value[highlightIdx.value]) {
    selectStock(suggestions.value[highlightIdx.value])
  } else if (suggestions.value.length > 0) {
    selectStock(suggestions.value[0])
  }
}

function hideSuggestions() {
  setTimeout(() => { showSuggestions.value = false }, 200)
}

async function selectStock(stock) {
  selectedStock.value = stock
  searchQuery.value = stock.name
  showSuggestions.value = false
  highlightIdx.value = -1
  searchError.value = ''
  await loadStockData(stock.code)
}

async function loadStockData(code) {
  try {
    const priceRes = await getStockPrice(code)
    stockPrice.value = priceRes.data
    await loadChart(code)
  } catch (e) {
    searchError.value = e.response?.data?.message || '주식 데이터를 불러오지 못했습니다.'
    stockPrice.value = null
  }
}

async function loadChart(code) {
  chartLoading.value = true
  const { startDate, endDate } = getDateRange(selectedPeriod.value.days)

  try {
    const res = await getStockChart(code, selectedPeriod.value.period, startDate, endDate)
    await nextTick()
    renderChart(res.data)
  } catch (e) {
    console.error('차트 로딩 실패', e)
  } finally {
    chartLoading.value = false
  }
}

function renderChart(data) {
  if (!chartContainer.value) return

  if (chart) {
    chart.remove()
    chart = null
    candleSeries = null
    volumeSeries = null
  }

  chart = createChart(chartContainer.value, {
    width: chartContainer.value.clientWidth,
    height: 460,
    layout: {
      background: { color: '#ffffff' },
      textColor: '#374151',
    },
    grid: {
      vertLines: { color: 'rgba(209, 213, 219, 0.5)' },
      horzLines: { color: 'rgba(209, 213, 219, 0.5)' },
    },
    crosshair: {
      mode: CrosshairMode.Normal,
    },
    rightPriceScale: {
      borderColor: 'rgba(209, 213, 219, 0.8)',
      scaleMargins: { top: 0.08, bottom: 0.28 },
    },
    timeScale: {
      borderColor: 'rgba(209, 213, 219, 0.8)',
      timeVisible: false,
      fixLeftEdge: true,
      fixRightEdge: true,
    },
    handleScroll: { vertTouchDrag: false },
  })

  // 캔들 시리즈 (한국: 빨강=상승, 파랑=하락)
  candleSeries = chart.addCandlestickSeries({
    upColor: '#ef5350',
    downColor: '#1976D2',
    borderUpColor: '#ef5350',
    borderDownColor: '#1976D2',
    wickUpColor: '#ef5350',
    wickDownColor: '#1976D2',
  })

  // 거래량 히스토그램
  volumeSeries = chart.addHistogramSeries({
    priceFormat: { type: 'volume' },
    priceScaleId: 'vol',
  })

  chart.priceScale('vol').applyOptions({
    scaleMargins: { top: 0.75, bottom: 0 },
  })

  const candleData = data.map(d => ({
    time: d.date,
    open: d.open,
    high: d.high,
    low: d.low,
    close: d.close,
  }))

  const volData = data.map(d => ({
    time: d.date,
    value: d.volume,
    color: d.close >= d.open ? 'rgba(239,83,80,0.4)' : 'rgba(25,118,210,0.4)',
  }))

  candleSeries.setData(candleData)
  volumeSeries.setData(volData)
  chart.timeScale().fitContent()
}

async function changePeriod(p) {
  selectedPeriod.value = p
  if (selectedStock.value) {
    await loadChart(selectedStock.value.code)
  }
}

function getDateRange(days) {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - days)
  return { startDate: fmtDate(start), endDate: fmtDate(end) }
}

function fmtDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}${m}${day}`
}

function formatVolume(v) {
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '억'
  if (v >= 10000) return (v / 10000).toFixed(1) + '만'
  return v.toLocaleString()
}

function handleLogout() {
  sessionStorage.removeItem('user')
  router.push('/')
}

function handleResize() {
  if (chart && chartContainer.value) {
    chart.applyOptions({ width: chartContainer.value.clientWidth })
  }
}

onMounted(() => window.addEventListener('resize', handleResize))
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) chart.remove()
})
</script>
