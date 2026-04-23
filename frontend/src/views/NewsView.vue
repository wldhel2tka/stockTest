<template>
  <div>
    <div class="container-fluid px-4 mt-4">
      <!-- 헤더 -->
      <div class="d-flex align-items-center justify-content-between mb-3">
        <div>
          <h5 class="fw-bold mb-0"><i class="bi bi-newspaper text-primary me-2"></i>뉴스 종목 분석</h5>
          <small class="text-muted">최신 한국 증시 뉴스에서 언급된 종목과 현재 시세</small>
        </div>
        <div class="d-flex gap-2 align-items-center">
          <span v-if="lastUpdated" class="text-muted small">
            <i class="bi bi-clock me-1"></i>{{ lastUpdated }}
          </span>
          <button class="btn btn-outline-primary btn-sm" @click="load(true)" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
            <i v-else class="bi bi-arrow-clockwise me-1"></i>새로고침
          </button>
        </div>
      </div>

      <!-- 건수 표시 -->
      <div class="mb-3">
        <span class="badge bg-primary me-1">{{ news.length }}건</span>
        <span class="text-muted small">종목 언급 기사</span>
      </div>

      <!-- 로딩 스켈레톤 -->
      <div v-if="loading && !news.length" class="row g-3">
        <div v-for="i in 6" :key="i" class="col-lg-6">
          <div class="card border-0 shadow-sm">
            <div class="card-body placeholder-glow">
              <div class="placeholder col-8 mb-2" style="height:16px;border-radius:4px;"></div>
              <div class="placeholder col-4 mb-3" style="height:12px;border-radius:4px;"></div>
              <div class="placeholder col-6" style="height:24px;border-radius:12px;"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 뉴스 그리드 -->
      <div v-else class="row g-3">
        <div v-for="item in news" :key="item.link" class="col-lg-6">
          <div class="card border-0 shadow-sm h-100 news-card"
               :class="{ 'border-start border-primary border-3': item.relatedStocks.length }">
            <div class="card-body py-3 px-3">
              <!-- 뉴스 제목 -->
              <div class="d-flex align-items-start gap-2 mb-2">
                <div class="flex-grow-1">
                  <a :href="item.link" target="_blank"
                     class="text-decoration-none text-dark fw-semibold news-title">
                    {{ item.title }}
                  </a>
                </div>
                <i v-if="item.relatedStocks.length"
                   class="bi bi-bar-chart-fill text-primary flex-shrink-0 mt-1"></i>
              </div>

              <!-- 출처 / 시간 -->
              <div class="d-flex align-items-center gap-2 mb-2">
                <span class="badge bg-light text-secondary border small">{{ item.source || 'Google News' }}</span>
                <span class="text-muted" style="font-size:0.75rem;">{{ item.publishedAt }}</span>
              </div>

              <!-- 관련 종목 뱃지 -->
              <div class="d-flex flex-wrap gap-1" style="max-height:80px;overflow:hidden;">
                <span v-for="s in item.relatedStocks" :key="s.code"
                      class="stock-badge d-inline-flex align-items-center gap-1 px-2 py-1 rounded-pill border"
                      :class="s.changeRate > 0 ? 'border-danger text-danger bg-danger bg-opacity-10'
                             : s.changeRate < 0 ? 'border-primary text-primary bg-primary bg-opacity-10'
                             : 'border-secondary text-secondary'"
                      style="font-size:0.78rem; cursor:pointer;"
                      @click="goToStock(s.code)">
                  <strong>{{ s.name }}</strong>
                  <span>{{ fmtMoney(s.currentPrice) }}</span>
                  <span class="fw-semibold">{{ s.changeRate > 0 ? '+' : '' }}{{ s.changeRate.toFixed(2) }}%</span>
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 추천 종목 요약 -->
      <div v-if="topStocks.length" class="mt-4">
        <div class="card border-0 shadow-sm">
          <div class="card-header bg-white fw-semibold py-2 px-3">
            <i class="bi bi-star-fill text-warning me-1"></i>뉴스 다수 언급 종목
            <small class="text-muted ms-1">(언급 빈도순)</small>
          </div>
          <div class="card-body p-0">
            <table class="table table-hover mb-0 small">
              <thead class="table-light">
                <tr>
                  <th class="ps-3">종목명</th>
                  <th class="text-end">현재가</th>
                  <th class="text-end">등락</th>
                  <th class="text-end pe-3">뉴스 언급</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in topStocks" :key="s.code" style="cursor:pointer;" @click="goToStock(s.code)">
                  <td class="ps-3 fw-semibold">
                    {{ s.name }}
                    <span class="text-muted ms-1" style="font-size:0.72rem;">{{ s.code }}</span>
                  </td>
                  <td class="text-end">{{ fmtMoney(s.currentPrice) }}원</td>
                  <td class="text-end" :class="s.changeRate > 0 ? 'text-danger' : s.changeRate < 0 ? 'text-primary' : ''">
                    <span class="badge" :class="s.changeRate > 0 ? 'bg-danger' : s.changeRate < 0 ? 'bg-primary' : 'bg-secondary'">
                      {{ s.changeRate > 0 ? '+' : '' }}{{ s.changeRate.toFixed(2) }}%
                    </span>
                  </td>
                  <td class="text-end pe-3">
                    <span class="badge bg-warning text-dark">{{ s.count }}건</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <div class="mt-3 p-3 rounded bg-white border text-muted small shadow-sm">
        <i class="bi bi-info-circle me-1"></i>
        Google News RSS에서 한국 증시 관련 최신 기사를 가져옵니다. 데이터는 30분간 캐시됩니다.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getNewsAnalysis } from '../api/index.js'

const router = useRouter()
const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

const news = ref([])
const loading = ref(false)
const lastUpdated = ref('')

const topStocks = computed(() => {
  const map = {}
  for (const item of news.value) {
    for (const s of item.relatedStocks) {
      if (!map[s.code]) map[s.code] = { ...s, count: 0 }
      map[s.code].count++
    }
  }
  return Object.values(map)
    .sort((a, b) => b.count - a.count)
    .slice(0, 10)
})

async function load(forceRefresh = false) {
  loading.value = true
  try {
    const res = await getNewsAnalysis(forceRefresh)
    news.value = res.data
    const now = new Date()
    lastUpdated.value = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')} 업데이트`
  } catch (e) {
    console.error('뉴스 로딩 실패', e)
  } finally {
    loading.value = false
  }
}

function fmtMoney(v) {
  return Math.round(v || 0).toLocaleString('ko-KR')
}

function goToStock(code) {
  router.push({ path: '/home', query: { code } })
}

onMounted(() => load())
</script>

<style scoped>
.news-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
  font-size: 0.9rem;
}
.news-card {
  transition: transform 0.15s;
}
.news-card:hover {
  transform: translateY(-2px);
}
.stock-badge {
  transition: opacity 0.15s;
}
.stock-badge:hover {
  opacity: 0.8;
}
</style>
