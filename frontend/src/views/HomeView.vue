<template>
  <div>
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
                      {{ fmtMoney(stockPrice.currentPrice) }}
                      <small class="fs-6 fw-normal">원</small>
                    </span>
                    <span class="fs-5 fw-semibold" :class="priceColorClass">
                      {{ stockPrice.change >= 0 ? '+' : '' }}{{ fmtMoney(stockPrice.change) }}
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
                        <div class="fw-semibold small">{{ fmtMoney(stockPrice.openPrice) }}</div>
                      </div>
                    </div>
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">고가</div>
                        <div class="fw-semibold small text-danger">{{ fmtMoney(stockPrice.highPrice) }}</div>
                      </div>
                    </div>
                    <div class="col-3">
                      <div class="bg-light rounded p-2">
                        <div class="text-muted" style="font-size: 0.7rem;">저가</div>
                        <div class="fw-semibold small text-primary">{{ fmtMoney(stockPrice.lowPrice) }}</div>
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

      <!-- 기술적 분석 신호 -->
      <div v-if="stockPrice" class="row mb-3">
        <div class="col-12">
          <div class="card border-0 shadow-sm">
            <div class="card-body py-2 px-3">
              <div v-if="signalLoading" class="d-flex align-items-center gap-2 text-muted small">
                <span class="spinner-border spinner-border-sm"></span> 기술적 분석 중...
              </div>
              <div v-else-if="signal" class="d-flex align-items-center gap-3 flex-wrap">
                <!-- 종합 신호 -->
                <span class="badge fs-6 px-3 py-2"
                  :class="signal.signal === 'BUY' ? 'bg-danger' : signal.signal === 'SELL' ? 'bg-primary' : 'bg-secondary'">
                  <i class="bi me-1"
                    :class="signal.signal === 'BUY' ? 'bi-arrow-up-circle-fill' : signal.signal === 'SELL' ? 'bi-arrow-down-circle-fill' : 'bi-dash-circle-fill'">
                  </i>
                  {{ signal.signal === 'BUY' ? '매수 우세' : signal.signal === 'SELL' ? '매도 우세' : '중립' }}
                </span>
                <!-- 지표들 -->
                <div class="d-flex gap-3 flex-wrap small">
                  <div class="d-flex align-items-center gap-1">
                    <span class="text-muted">RSI</span>
                    <span class="fw-bold"
                      :class="signal.rsi <= 30 ? 'text-danger' : signal.rsi >= 70 ? 'text-primary' : 'text-dark'">
                      {{ signal.rsi?.toFixed(1) }}
                    </span>
                    <span class="badge badge-sm rounded-pill"
                      :class="signal.rsiSignal === 'BUY' ? 'bg-danger' : signal.rsiSignal === 'SELL' ? 'bg-primary' : 'bg-light text-dark'">
                      {{ signal.rsi <= 30 ? '과매도' : signal.rsi >= 70 ? '과매수' : '중립' }}
                    </span>
                  </div>
                  <div class="d-flex align-items-center gap-1">
                    <span class="text-muted">이동평균</span>
                    <span class="fw-bold"
                      :class="signal.maSignal === 'BUY' ? 'text-danger' : signal.maSignal === 'SELL' ? 'text-primary' : 'text-dark'">
                      5MA {{ signal.ma5 > signal.ma20 ? '▲' : signal.ma5 < signal.ma20 ? '▼' : '—' }} 20MA
                    </span>
                  </div>
                  <div class="d-flex align-items-center gap-1">
                    <span class="text-muted">거래량</span>
                    <span class="fw-bold" :class="signal.volumeSignal === 'HIGH' ? 'text-warning' : 'text-dark'">
                      {{ signal.volumeRatio?.toFixed(1) }}배
                    </span>
                    <span v-if="signal.volumeSignal === 'HIGH'" class="badge bg-warning text-dark rounded-pill">급등</span>
                  </div>
                </div>
                <div class="text-muted small ms-auto">{{ signal.description }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 매수/매도 영역 -->
      <div v-if="stockPrice" class="row mb-3">
        <div class="col-12">
          <div class="card border-0 shadow-sm">
            <div class="card-body py-2 px-3">
              <div class="d-flex align-items-center gap-3 flex-wrap">
                <div class="d-flex gap-2">
                  <button class="btn btn-danger px-4" @click="openTradeModal('BUY')">
                    <i class="bi bi-cart-plus me-1"></i>매수
                  </button>
                  <button class="btn btn-primary px-4"
                          :disabled="!userHolding || userHolding.quantity === 0"
                          @click="openTradeModal('SELL')">
                    <i class="bi bi-cart-dash me-1"></i>매도
                  </button>
                </div>
                <div v-if="userHolding && userHolding.quantity > 0" class="text-muted small border-start ps-3">
                  보유 <strong class="text-dark">{{ userHolding.quantity }}주</strong>
                  &nbsp;|&nbsp; 평균단가 <strong class="text-dark">{{ fmtMoney(userHolding.avgPrice) }}원</strong>
                  &nbsp;|&nbsp; 평가손익
                  <strong :class="(stockPrice.currentPrice - userHolding.avgPrice) * userHolding.quantity >= 0 ? 'text-danger' : 'text-primary'">
                    {{ fmtMoney((stockPrice.currentPrice - userHolding.avgPrice) * userHolding.quantity) }}원
                  </strong>
                </div>
                <div v-else class="text-muted small border-start ps-3">보유 수량 없음</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 차트 + 추천 종목 사이드바 -->
      <div class="row g-3">
        <!-- 차트 -->
        <div class="col-lg-8 d-flex flex-column gap-3">
          <div v-if="selectedStock" class="card border-0 shadow-sm">
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
          <div v-else class="text-center py-5 text-muted">
            <i class="bi bi-bar-chart-line display-1 opacity-25"></i>
            <p class="mt-3 fs-5">종목을 검색하여 주가 차트를 확인하세요</p>
            <p class="small">KOSPI / KOSDAQ 주요 종목 지원</p>
          </div>

          <!-- 실시간 체결 -->
          <div v-if="selectedStock" class="card border-0 shadow-sm">
            <div class="card-header bg-white d-flex justify-content-between align-items-center py-2 px-3">
              <span class="fw-semibold text-secondary small">
                <i class="bi bi-activity me-1"></i>실시간 체결
                <span class="badge bg-danger ms-1 rounded-pill" style="font-size:0.6rem;animation:blink 1.2s infinite">LIVE</span>
              </span>
              <span class="text-muted" style="font-size:0.75rem">최근 {{ trades.length }}건 표시</span>
            </div>
            <div class="card-body p-0">
              <div style="height:220px; overflow-y:auto;" ref="tradesContainer">
                <table class="table table-sm table-hover mb-0" style="font-size:0.8rem;">
                  <thead class="table-light" style="position:sticky;top:0;z-index:1">
                    <tr>
                      <th class="fw-normal text-muted py-1 ps-3" style="width:70px">시간</th>
                      <th class="fw-normal text-muted py-1 text-end" style="width:100px">체결가</th>
                      <th class="fw-normal text-muted py-1 text-end" style="width:80px">등락</th>
                      <th class="fw-normal text-muted py-1 text-end" style="width:80px">체결량</th>
                      <th class="fw-normal text-muted py-1 text-center pe-3" style="width:60px">구분</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(t, i) in trades" :key="i" :class="i === 0 ? 'table-active' : ''">
                      <td class="text-muted py-1 ps-3">{{ t.time }}</td>
                      <td class="py-1 text-end fw-semibold" :class="t.side === '매수' ? 'text-danger' : t.side === '매도' ? 'text-primary' : 'text-muted'">
                        {{ fmtMoney(t.price) }}
                      </td>
                      <td class="py-1 text-end small" :class="t.changeRate > 0 ? 'text-danger' : t.changeRate < 0 ? 'text-primary' : 'text-muted'">
                        {{ t.changeRate > 0 ? '+' : '' }}{{ t.changeRate.toFixed(2) }}%
                      </td>
                      <td class="py-1 text-end text-muted">{{ t.volume.toLocaleString() }}</td>
                      <td class="py-1 text-center pe-3">
                        <span class="badge rounded-pill"
                          :class="t.side === '매수' ? 'bg-danger' : t.side === '매도' ? 'bg-primary' : 'bg-secondary'"
                          style="font-size:0.62rem">{{ t.side }}</span>
                      </td>
                    </tr>
                  </tbody>
                </table>
                <div v-if="trades.length === 0" class="text-center py-4 text-muted small">
                  <span class="spinner-border spinner-border-sm me-2"></span>체결 데이터 수신 대기 중...
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 추천 종목 사이드바 -->
        <div class="col-lg-4">
          <div class="card border-0 shadow-sm h-100">
            <div class="card-header bg-white py-2 px-3 d-flex justify-content-between align-items-center">
              <span class="fw-semibold text-secondary small">
                <i class="bi bi-star-fill text-warning me-1"></i>추천 종목
              </span>
              <div v-if="rankTab !== 'sector'" class="btn-group btn-group-sm">
                <button class="btn" :class="rankMarket === 'J' ? 'btn-primary' : 'btn-outline-secondary'" @click="setRankMarket('J')" style="font-size:0.7rem;padding:1px 6px">KOSPI</button>
                <button class="btn" :class="rankMarket === 'Q' ? 'btn-primary' : 'btn-outline-secondary'" @click="setRankMarket('Q')" style="font-size:0.7rem;padding:1px 6px">KOSDAQ</button>
              </div>
              <span v-else class="text-muted" style="font-size:0.72rem;">{{ selectedSector?.name }}</span>
            </div>
            <div class="card-header bg-white border-top-0 py-0 px-0">
              <ul class="nav nav-tabs border-0 px-3" style="font-size:0.8rem">
                <li class="nav-item">
                  <button class="nav-link py-2 px-2" :class="{ active: rankTab === 'volume' }" @click="setRankTab('volume')">거래량</button>
                </li>
                <li class="nav-item">
                  <button class="nav-link py-2 px-2 text-danger" :class="{ active: rankTab === 'gainers' }" @click="setRankTab('gainers')">상승</button>
                </li>
                <li class="nav-item">
                  <button class="nav-link py-2 px-2 text-primary" :class="{ active: rankTab === 'losers' }" @click="setRankTab('losers')">하락</button>
                </li>
                <li class="nav-item">
                  <button class="nav-link py-2 px-2 text-success" :class="{ active: rankTab === 'sector' }" @click="setRankTab('sector')">섹터</button>
                </li>
              </ul>
            </div>
            <!-- 섹터 선택 -->
            <div v-if="rankTab === 'sector'" class="card-header bg-light border-top-0 py-2 px-3">
              <div class="d-flex flex-wrap gap-1">
                <button
                  v-for="sec in sectors"
                  :key="sec.name"
                  class="btn btn-sm"
                  :class="selectedSector?.name === sec.name ? 'btn-primary' : 'btn-outline-secondary'"
                  style="font-size:0.72rem;padding:2px 8px"
                  @click="loadSectorStocks(sec)"
                >
                  <i :class="sec.icon + ' me-1'"></i>{{ sec.name }}
                </button>
              </div>
            </div>
            <!-- 필터 -->
            <div v-if="rankTab !== 'sector'" class="card-header bg-light border-top-0 py-2 px-3">
              <div class="row g-1 align-items-center" style="font-size:0.78rem">
                <div class="col-12 text-muted mb-1" style="font-size:0.72rem">필터</div>
                <div class="col-6">
                  <div class="input-group input-group-sm">
                    <span class="input-group-text py-0" style="font-size:0.72rem">최저가</span>
                    <input v-model.number="filterMinPrice" type="number" min="0" class="form-control py-0" style="font-size:0.72rem" placeholder="0" @keydown.enter="loadRank" />
                  </div>
                </div>
                <div class="col-6">
                  <div class="input-group input-group-sm">
                    <span class="input-group-text py-0" style="font-size:0.72rem">최고가</span>
                    <input v-model.number="filterMaxPrice" type="number" min="0" class="form-control py-0" style="font-size:0.72rem" placeholder="0" @keydown.enter="loadRank" />
                  </div>
                </div>
                <div class="col-8 mt-1">
                  <div class="input-group input-group-sm">
                    <span class="input-group-text py-0" style="font-size:0.72rem">최소거래량</span>
                    <input v-model.number="filterMinVolume" type="number" min="0" class="form-control py-0" style="font-size:0.72rem" placeholder="0" @keydown.enter="loadRank" />
                  </div>
                </div>
                <div class="col-4 mt-1 d-flex gap-1">
                  <button class="btn btn-primary btn-sm w-50 py-0" style="font-size:0.72rem" @click="loadRank">적용</button>
                  <button class="btn btn-outline-secondary btn-sm w-50 py-0" style="font-size:0.72rem" @click="resetFilter">초기화</button>
                </div>
              </div>
            </div>
            <div class="card-body p-0" style="overflow-y:auto; max-height:430px">
              <div v-if="rankLoading" class="text-center py-4">
                <div class="spinner-border spinner-border-sm text-primary"></div>
              </div>
              <ul v-else class="list-group list-group-flush">
                <li
                  v-for="s in rankStocks"
                  :key="s.code"
                  class="list-group-item list-group-item-action px-3 py-2"
                  style="cursor:pointer; font-size:0.82rem"
                  @click="selectStockByCode(s)"
                >
                  <div class="d-flex justify-content-between align-items-center">
                    <div class="d-flex align-items-center gap-2">
                      <span class="text-muted" style="width:18px;font-size:0.75rem">{{ s.rank }}</span>
                      <span class="fw-semibold">{{ s.name }}</span>
                    </div>
                    <div class="text-end">
                      <div class="fw-semibold" :class="s.up ? 'text-danger' : (s.changeRate < 0 ? 'text-primary' : 'text-muted')">
                        {{ s.changeRate >= 0 ? '+' : '' }}{{ s.changeRate.toFixed(2) }}%
                      </div>
                      <div class="text-muted" style="font-size:0.75rem">{{ fmtMoney(s.currentPrice) }}원</div>
                    </div>
                  </div>
                </li>
                <li v-if="!rankStocks.length && !rankLoading" class="list-group-item text-center text-muted py-4 small">
                  데이터가 없습니다.
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      <!-- 거래 모달 -->
      <Teleport to="body">
        <div v-if="showTradeModal"
             class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
             style="background: rgba(0,0,0,0.5); z-index: 1055;"
             @click.self="closeTradeModal">
          <div class="card shadow-lg" style="width: 360px;">
            <div class="card-header d-flex justify-content-between align-items-center py-2"
                 :class="tradeType === 'BUY' ? 'bg-danger text-white' : 'bg-primary text-white'">
              <span class="fw-bold">
                {{ tradeType === 'BUY' ? '매수' : '매도' }} — {{ stockPrice?.name }}
              </span>
              <button class="btn-close btn-close-white btn-sm" @click="closeTradeModal"></button>
            </div>
            <div class="card-body">
              <div class="d-flex justify-content-between mb-2">
                <span class="text-muted small">현재가</span>
                <strong>{{ fmtMoney(stockPrice?.currentPrice) }}원</strong>
              </div>
              <div v-if="tradeType === 'BUY'" class="d-flex justify-content-between mb-2">
                <span class="text-muted small">가용 잔고</span>
                <strong>{{ fmtMoney(account?.balance) }}원</strong>
              </div>
              <div v-else class="d-flex justify-content-between mb-2">
                <span class="text-muted small">보유 수량</span>
                <strong>{{ userHolding?.quantity || 0 }}주</strong>
              </div>
              <hr class="my-2"/>
              <div class="mb-3">
                <label class="form-label fw-semibold small">수량</label>
                <div class="input-group input-group-sm">
                  <button class="btn btn-outline-secondary" @click="tradeQty = Math.max(1, tradeQty - 1)">−</button>
                  <input v-model.number="tradeQty" type="number" min="1" class="form-control text-center fw-bold"/>
                  <button class="btn btn-outline-secondary" @click="tradeQty++">+</button>
                </div>
                <div v-if="tradeType === 'BUY'" class="text-end mt-1">
                  <small class="text-muted">최대 {{ maxBuyQty }}주 매수 가능</small>
                </div>
              </div>
              <div class="d-flex justify-content-between mb-1">
                <span class="text-muted small">주문금액</span>
                <strong :class="tradeType === 'BUY' ? 'text-danger' : 'text-primary'">
                  {{ fmtMoney(tradeQty * (stockPrice?.currentPrice || 0)) }}원
                </strong>
              </div>
              <div v-if="tradeType === 'BUY'" class="d-flex justify-content-between">
                <span class="text-muted small">매수 후 잔고</span>
                <span class="small">{{ fmtMoney(Math.max(0, (account?.balance || 0) - tradeQty * (stockPrice?.currentPrice || 0))) }}원</span>
              </div>
              <div v-if="tradeError" class="alert alert-danger py-1 px-2 mt-2 small mb-0">
                <i class="bi bi-exclamation-circle me-1"></i>{{ tradeError }}
              </div>
            </div>
            <div class="card-footer d-flex gap-2 py-2">
              <button class="btn btn-secondary flex-fill btn-sm" @click="closeTradeModal">취소</button>
              <button class="btn flex-fill btn-sm fw-bold"
                      :class="tradeType === 'BUY' ? 'btn-danger' : 'btn-primary'"
                      :disabled="tradeLoading"
                      @click="executeTrade">
                <span v-if="tradeLoading" class="spinner-border spinner-border-sm me-1"></span>
                {{ tradeType === 'BUY' ? '매수 확인' : '매도 확인' }}
              </button>
            </div>
          </div>
        </div>
      </Teleport>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { searchStocks, getStockPrice, getStockChart, getMinuteChart, getAccount, getPortfolio, buyStock, sellStock, getTopVolume, getGainers, getLosers, getSignal } from '../api/index.js'
import { createChart, CrosshairMode } from 'lightweight-charts'

const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

// 계좌 / 보유 종목
const account = ref(null)
const userHolding = ref(null)

// 거래 모달
const showTradeModal = ref(false)
const tradeType = ref('BUY')
const tradeQty = ref(1)
const tradeLoading = ref(false)
const tradeError = ref('')

const maxBuyQty = computed(() => {
  if (!account.value || !stockPrice.value) return 0
  return Math.floor(account.value.balance / stockPrice.value.currentPrice)
})

// 추천 종목 사이드바
const rankTab = ref('volume')
const rankMarket = ref('J')
const rankStocks = ref([])
const rankLoading = ref(false)
const filterMinPrice = ref(0)
const filterMaxPrice = ref(0)
const filterMinVolume = ref(0)

// 섹터
const sectors = [
  { name: '반도체', icon: 'bi bi-cpu', stocks: [
    { code: '005930', name: '삼성전자' },
    { code: '000660', name: 'SK하이닉스' },
    { code: '042700', name: '한미반도체' },
    { code: '058470', name: '리노공업' },
    { code: '240810', name: '원익IPS' },
    { code: '403870', name: 'HPSP' },
  ]},
  { name: '2차전지', icon: 'bi bi-battery-charging', stocks: [
    { code: '373220', name: 'LG에너지솔루션' },
    { code: '006400', name: '삼성SDI' },
    { code: '247540', name: '에코프로비엠' },
    { code: '086520', name: '에코프로' },
    { code: '003670', name: '포스코퓨처엠' },
  ]},
  { name: 'AI/플랫폼', icon: 'bi bi-robot', stocks: [
    { code: '035420', name: 'NAVER' },
    { code: '035720', name: '카카오' },
    { code: '259960', name: '크래프톤' },
    { code: '112040', name: '위메이드' },
    { code: '263750', name: '펄어비스' },
  ]},
  { name: '바이오', icon: 'bi bi-heart-pulse', stocks: [
    { code: '207940', name: '삼성바이오로직스' },
    { code: '068270', name: '셀트리온' },
    { code: '128940', name: '한미약품' },
    { code: '000100', name: '유한양행' },
    { code: '185750', name: '종근당' },
  ]},
  { name: '자동차', icon: 'bi bi-car-front', stocks: [
    { code: '005380', name: '현대차' },
    { code: '000270', name: '기아' },
    { code: '012330', name: '현대모비스' },
    { code: '204320', name: 'HL만도' },
    { code: '018880', name: '한온시스템' },
  ]},
  { name: '금융', icon: 'bi bi-bank', stocks: [
    { code: '105560', name: 'KB금융' },
    { code: '055550', name: '신한지주' },
    { code: '086790', name: '하나금융지주' },
    { code: '316140', name: '우리금융지주' },
    { code: '000810', name: '삼성화재' },
  ]},
]
const selectedSector = ref(null)

async function loadSectorStocks(sector) {
  selectedSector.value = sector
  rankLoading.value = true
  try {
    const results = await Promise.all(
      sector.stocks.map((s, idx) =>
        getStockPrice(s.code)
          .then(r => ({
            rank: idx + 1,
            code: s.code,
            name: r.data.name || s.name,
            currentPrice: r.data.currentPrice,
            changeRate: r.data.changeRate,
            up: r.data.up,
          }))
          .catch(() => null)
      )
    )
    rankStocks.value = results.filter(Boolean)
  } catch {
    rankStocks.value = []
  } finally {
    rankLoading.value = false
  }
}

async function loadRank() {
  rankLoading.value = true
  const params = {
    market: rankMarket.value,
    minPrice: filterMinPrice.value || 0,
    maxPrice: filterMaxPrice.value || 0,
    minVolume: filterMinVolume.value || 0,
  }
  try {
    let res
    if (rankTab.value === 'volume') res = await getTopVolume(params.market, params.minPrice, params.maxPrice, params.minVolume)
    else if (rankTab.value === 'gainers') res = await getGainers(params.market, params.minPrice, params.maxPrice, params.minVolume)
    else res = await getLosers(params.market, params.minPrice, params.maxPrice, params.minVolume)
    rankStocks.value = res.data.slice(0, 15)
  } catch {
    rankStocks.value = []
  } finally {
    rankLoading.value = false
  }
}

function setRankTab(t) {
  rankTab.value = t
  if (t === 'sector') {
    loadSectorStocks(selectedSector.value ?? sectors[0])
  } else {
    loadRank()
  }
}
function setRankMarket(m) { rankMarket.value = m; loadRank() }
function resetFilter() {
  filterMinPrice.value = 0
  filterMaxPrice.value = 0
  filterMinVolume.value = 0
  loadRank()
}

async function selectStockByCode(s) {
  searchQuery.value = s.name
  selectedStock.value = { code: s.code, name: s.name }
  showSuggestions.value = false
  await loadStockData(s.code)
}

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

// 기술적 분석 신호
const signal = ref(null)
const signalLoading = ref(false)

// 실시간 체결
const trades = ref([])
const tradesContainer = ref(null)
let eventSource = null
const realtimeCandle = ref(null) // { time, open, high, low, close, volume }

function subscribeRealtime(code) {
  if (eventSource) { eventSource.close(); eventSource = null }
  trades.value = []
  eventSource = new EventSource(`/api/realtime/stream?code=${code}`)
  eventSource.onmessage = (e) => {
    const trade = JSON.parse(e.data)
    trades.value.unshift(trade)
    if (trades.value.length > 10) trades.value.pop()
    updateChartRealtime(trade)
  }
  eventSource.onerror = () => {
    // 브라우저가 자동 재연결
  }
}

function todayStr() {
  const d = new Date()
  return d.getFullYear() + '-'
    + String(d.getMonth() + 1).padStart(2, '0') + '-'
    + String(d.getDate()).padStart(2, '0')
}

function currentMinuteTs() {
  const now = new Date()
  now.setSeconds(0, 0)
  return Math.floor(now.getTime() / 1000)
}

function updateChartRealtime(trade) {
  if (!candleSeries || !volumeSeries) return

  const isIntraday = selectedPeriod.value.type === 'minute'
  const newKey = isIntraday ? currentMinuteTs() : todayStr()
  const c = realtimeCandle.value

  if (!c || c.timeKey !== newKey) {
    realtimeCandle.value = {
      timeKey: newKey, time: newKey,
      open: trade.price, high: trade.price, low: trade.price, close: trade.price,
      volume: trade.volume,
    }
  } else {
    realtimeCandle.value = {
      ...c,
      high: Math.max(c.high, trade.price),
      low: Math.min(c.low, trade.price),
      close: trade.price,
      volume: c.volume + trade.volume,
    }
  }

  const u = realtimeCandle.value
  candleSeries.update({ time: u.time, open: u.open, high: u.high, low: u.low, close: u.close })
  volumeSeries.update({
    time: u.time, value: u.volume,
    color: u.close >= u.open ? 'rgba(239,83,80,0.4)' : 'rgba(25,118,210,0.4)',
  })
}

// 차트
const chartContainer = ref(null)
const chartLoading = ref(false)
let chart = null
let candleSeries = null
let volumeSeries = null

// 기간 옵션
const periodOptions = [
  { key: '1H', label: '1시간', type: 'minute', minuteType: '1H' },
  { key: '1D', label: '1일',   type: 'minute', minuteType: '1D' },
  { key: '1W', label: '1주일', type: 'daily',  period: 'D', days: 14 },
]
const selectedPeriod = ref(periodOptions[1]) // 기본 1일

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
  signal.value = null
  try {
    const priceRes = await getStockPrice(code)
    stockPrice.value = priceRes.data
    await loadChart(code)
    await loadUserHolding(code)
    subscribeRealtime(code)
    loadSignal(code)
  } catch (e) {
    searchError.value = e.response?.data?.message || '주식 데이터를 불러오지 못했습니다.'
    stockPrice.value = null
  }
}

async function loadUserHolding(code) {
  if (!user.value?.id) return
  try {
    const res = await getAccount(user.value.id)
    account.value = res.data
    // portfolio에서 현재 종목 찾기 (별도 포트폴리오 조회 없이 account로 대체)
  } catch { /* silent */ }
  // 보유 종목은 포트폴리오 API에서 가져옴
  try {
    const res = await getPortfolio(user.value.id)
    userHolding.value = res.data.holdings.find(h => h.stockCode === code) || null
  } catch { /* silent */ }
}

async function loadSignal(code) {
  signalLoading.value = true
  try {
    const res = await getSignal(code)
    signal.value = res.data
  } catch { signal.value = null }
  finally { signalLoading.value = false }
}

// 거래 모달
function openTradeModal(type) {
  tradeType.value = type
  tradeQty.value = 1
  tradeError.value = ''
  showTradeModal.value = true
}

function closeTradeModal() {
  showTradeModal.value = false
  tradeError.value = ''
}

async function executeTrade() {
  if (!user.value?.id || !selectedStock.value) return
  if (tradeQty.value < 1) { tradeError.value = '수량을 1 이상 입력하세요.'; return }

  tradeLoading.value = true
  tradeError.value = ''
  try {
    if (tradeType.value === 'BUY') {
      await buyStock(user.value.id, selectedStock.value.code, tradeQty.value)
    } else {
      await sellStock(user.value.id, selectedStock.value.code, tradeQty.value)
    }
    closeTradeModal()
    // 잔고 및 보유 종목 갱신
    await loadUserHolding(selectedStock.value.code)
  } catch (e) {
    tradeError.value = e.response?.data?.message || '거래 중 오류가 발생했습니다.'
  } finally {
    tradeLoading.value = false
  }
}

async function loadChart(code) {
  chartLoading.value = true
  try {
    let res
    if (selectedPeriod.value.type === 'minute') {
      res = await getMinuteChart(code, selectedPeriod.value.minuteType)
    } else {
      const { startDate, endDate } = getDateRange(selectedPeriod.value.days)
      res = await getStockChart(code, selectedPeriod.value.period, startDate, endDate)
    }
    chartLoading.value = false
    await nextTick()
    renderChart(res.data, selectedPeriod.value.type === 'minute')
  } catch (e) {
    console.error('차트 로딩 실패', e)
    chartLoading.value = false
  }
}

function renderChart(data, isIntraday = false) {
  if (!chartContainer.value) return

  const containerWidth = chartContainer.value.clientWidth || chartContainer.value.offsetWidth || 800

  if (chart) {
    chart.remove()
    chart = null
    candleSeries = null
    volumeSeries = null
  }

  chart = createChart(chartContainer.value, {
    width: containerWidth,
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
      timeVisible: isIntraday,
      secondsVisible: false,
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
    priceFormat: { type: 'price', precision: 0, minMove: 1 },
  })

  // 거래량 히스토그램
  volumeSeries = chart.addHistogramSeries({
    priceFormat: { type: 'volume' },
    priceScaleId: 'vol',
  })

  chart.priceScale('vol').applyOptions({
    scaleMargins: { top: 0.75, bottom: 0 },
  })

  const timeOf = d => d.timestamp ?? d.date

  const candleData = data.map(d => ({
    time: timeOf(d),
    open: d.open,
    high: d.high,
    low: d.low,
    close: d.close,
  }))

  const volData = data.map(d => ({
    time: timeOf(d),
    value: d.volume,
    color: d.close >= d.open ? 'rgba(239,83,80,0.4)' : 'rgba(25,118,210,0.4)',
  }))

  candleSeries.setData(candleData)
  volumeSeries.setData(volData)
  chart.timeScale().fitContent()

  // 실시간 업데이트를 위한 마지막 캔들 상태 저장
  if (data.length > 0) {
    const last = data[data.length - 1]
    const timeKey = last.timestamp ?? last.date
    realtimeCandle.value = {
      timeKey,
      time: timeKey,
      open: last.open, high: last.high, low: last.low, close: last.close, volume: last.volume,
    }
  } else {
    realtimeCandle.value = null
  }
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

function fmtMoney(v) {
  return Math.round(v || 0).toLocaleString('ko-KR')
}

function formatVolume(v) {
  if (v >= 100000000) return (v / 100000000).toFixed(1) + '억'
  if (v >= 10000) return (v / 10000).toFixed(1) + '만'
  return Math.round(v || 0).toLocaleString('ko-KR')
}

function handleResize() {
  if (chart && chartContainer.value) {
    chart.applyOptions({ width: chartContainer.value.clientWidth })
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  if (user.value?.id) {
    try {
      const res = await getAccount(user.value.id)
      account.value = res.data
    } catch { /* silent */ }
  }
  loadRank()
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) chart.remove()
  if (eventSource) eventSource.close()
})
</script>

<style scoped>
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
