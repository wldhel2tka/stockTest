<template>
  <div class="container-fluid px-4 mt-4 pb-5">
    <!-- 헤더 -->
    <div class="d-flex align-items-center justify-content-between mb-4">
      <div>
        <h5 class="fw-bold mb-0"><i class="bi bi-calendar-event text-primary me-2"></i>공모주 일정</h5>
        <small class="text-muted">2026년 청약 예정 공모주 및 주관 증권사</small>
      </div>
      <div class="d-flex gap-2">
        <button
          v-for="m in months"
          :key="m.value"
          class="btn btn-sm"
          :class="activeMonth === m.value ? 'btn-primary' : 'btn-outline-secondary'"
          @click="activeMonth = m.value"
        >{{ m.label }}</button>
      </div>
    </div>

    <!-- 오늘 기준 안내 -->
    <div class="alert alert-info py-2 px-3 d-flex align-items-center gap-2 mb-3" style="font-size:0.85rem;">
      <i class="bi bi-info-circle-fill"></i>
      오늘({{ todayStr }}) 이후 청약 예정 종목만 표시됩니다. 데이터는 38커뮤니케이션 등 공개 정보 기준이며 변동될 수 있습니다.
    </div>

    <!-- 목록 없음 -->
    <div v-if="filteredIpos.length === 0" class="text-center py-5 text-muted">
      <i class="bi bi-calendar-x display-3 opacity-25"></i>
      <p class="mt-3">해당 월의 공모주 일정이 없습니다.</p>
    </div>

    <!-- 카드 목록 -->
    <div v-else class="row g-3">
      <div v-for="ipo in filteredIpos" :key="ipo.name" class="col-12 col-lg-6">
        <div
          class="card border-0 shadow-sm h-100"
          :class="{ 'opacity-50': isPast(ipo.endDate) }"
        >
          <div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <div>
                <h6 class="fw-bold mb-0">{{ ipo.name }}</h6>
                <small class="text-muted">{{ ipo.sector }}</small>
              </div>
              <div class="d-flex gap-1 flex-wrap justify-content-end">
                <span class="badge rounded-pill" :class="ipo.market === 'KOSPI' ? 'bg-primary' : 'bg-success'" style="font-size:0.7rem;">
                  {{ ipo.market }}
                </span>
                <span v-if="isToday(ipo.startDate, ipo.endDate)" class="badge rounded-pill bg-danger" style="font-size:0.7rem; animation: blink 1.2s infinite;">
                  청약중
                </span>
                <span v-else-if="isPast(ipo.endDate)" class="badge rounded-pill bg-secondary" style="font-size:0.7rem;">
                  청약완료
                </span>
                <span v-else class="badge rounded-pill bg-warning text-dark" style="font-size:0.7rem;">
                  청약예정
                </span>
              </div>
            </div>

            <div class="row g-2 mt-1" style="font-size:0.83rem;">
              <div class="col-6">
                <div class="text-muted" style="font-size:0.72rem;">청약일</div>
                <div class="fw-semibold">{{ ipo.startDate }} ~ {{ ipo.endDate }}</div>
              </div>
              <div class="col-6">
                <div class="text-muted" style="font-size:0.72rem;">환불일</div>
                <div class="fw-semibold">{{ ipo.refundDate || '-' }}</div>
              </div>
              <div class="col-6">
                <div class="text-muted" style="font-size:0.72rem;">희망 공모가</div>
                <div class="fw-semibold text-primary">{{ ipo.priceRange }}</div>
              </div>
              <div class="col-6">
                <div class="text-muted" style="font-size:0.72rem;">상장 예정일</div>
                <div class="fw-semibold">{{ ipo.listingDate || '미정' }}</div>
              </div>
            </div>

            <!-- 주관사 -->
            <div class="mt-2 pt-2 border-top">
              <div class="text-muted mb-1" style="font-size:0.72rem;">주관 증권사</div>
              <div class="d-flex flex-wrap gap-1">
                <span
                  v-for="broker in ipo.brokers"
                  :key="broker"
                  class="badge bg-light text-dark border"
                  style="font-size:0.72rem;"
                >
                  {{ broker }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const today = new Date('2026-04-23')
const todayStr = '2026-04-23'

const months = [
  { label: '4월', value: 4 },
  { label: '5월', value: 5 },
  { label: '6월', value: 6 },
]
const activeMonth = ref(4)

const ipos = [
  // 4월
  {
    name: '키움히어로스팩2호',
    sector: '금융 지원 서비스업',
    market: 'KOSDAQ',
    month: 4,
    startDate: '4/14',
    endDate: '4/15',
    refundDate: '4/17',
    priceRange: '2,000원',
    listingDate: '미정',
    brokers: ['키움증권'],
  },
  {
    name: '채비 (구.대영채비)',
    sector: '전기차 충전 솔루션',
    market: 'KOSDAQ',
    month: 4,
    startDate: '4/20',
    endDate: '4/21',
    refundDate: '4/23',
    priceRange: '12,300 ~ 15,300원',
    listingDate: '미정',
    brokers: ['KB증권', '삼성증권', '대신증권', '하나투자증권'],
  },
  {
    name: '신한스팩18호',
    sector: '금융 지원 서비스업',
    market: 'KOSDAQ',
    month: 4,
    startDate: '4/20',
    endDate: '4/21',
    refundDate: '4/23',
    priceRange: '2,000원',
    listingDate: '미정',
    brokers: ['신한투자증권'],
  },
  {
    name: '코스모로보틱스',
    sector: '의료용 기기 제조업',
    market: 'KOSDAQ',
    month: 4,
    startDate: '4/27',
    endDate: '4/28',
    refundDate: '4/30',
    priceRange: '5,300 ~ 6,000원',
    listingDate: '미정',
    brokers: ['미래에셋증권', '삼성증권'],
  },
  // 5월
  {
    name: '폴레드',
    sector: '유아동 용품',
    market: 'KOSDAQ',
    month: 5,
    startDate: '5/4',
    endDate: '5/6',
    refundDate: '5/8',
    priceRange: '4,100 ~ 5,000원',
    listingDate: '미정',
    brokers: ['NH투자증권'],
  },
  {
    name: '스트라드비젼',
    sector: '자동차용 AI 소프트웨어',
    market: 'KOSDAQ',
    month: 5,
    startDate: '5/11',
    endDate: '5/12',
    refundDate: '5/14',
    priceRange: '12,400 ~ 14,800원',
    listingDate: '미정',
    brokers: ['KB증권'],
  },
  {
    name: '마키나락스',
    sector: '산업 AI 솔루션',
    market: 'KOSDAQ',
    month: 5,
    startDate: '5/11',
    endDate: '5/12',
    refundDate: '5/14',
    priceRange: '12,500 ~ 15,000원',
    listingDate: '미정',
    brokers: ['미래에셋증권', '현대차증권'],
  },
  {
    name: '대신밸런스스팩20호',
    sector: '금융 지원 서비스업',
    market: 'KOSDAQ',
    month: 5,
    startDate: '5/12',
    endDate: '5/13',
    refundDate: '5/15',
    priceRange: '2,000원',
    listingDate: '미정',
    brokers: ['대신증권'],
  },
  {
    name: '피스피스스튜디오',
    sector: '편조의복 제조업',
    market: 'KOSDAQ',
    month: 5,
    startDate: '5/20',
    endDate: '5/21',
    refundDate: '5/26',
    priceRange: '19,000 ~ 21,500원',
    listingDate: '미정',
    brokers: ['NH투자증권', '이베스트증권'],
  },
  // 6월
  {
    name: '리테일아이',
    sector: '소프트웨어 개발',
    market: 'KOSDAQ',
    month: 6,
    startDate: '5/29',
    endDate: '6/1',
    refundDate: '6/3',
    priceRange: '10,500 ~ 12,500원',
    listingDate: '미정',
    brokers: ['삼성증권'],
  },
  {
    name: '한국데이터센터',
    sector: '데이터센터 운영',
    market: 'KOSPI',
    month: 6,
    startDate: '6/1',
    endDate: '6/2',
    refundDate: '6/4',
    priceRange: '7,500 ~ 10,000원',
    listingDate: '미정',
    brokers: ['KB증권'],
  },
]

function parseDate(str) {
  const [m, d] = str.split('/').map(Number)
  return new Date(2026, m - 1, d)
}

function isPast(endDateStr) {
  return parseDate(endDateStr) < today
}

function isToday(startDateStr, endDateStr) {
  const start = parseDate(startDateStr)
  const end = parseDate(endDateStr)
  return start <= today && today <= end
}

const filteredIpos = computed(() =>
  ipos.filter(i => i.month === activeMonth.value && !isPast(i.endDate))
)
</script>

<style scoped>
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
