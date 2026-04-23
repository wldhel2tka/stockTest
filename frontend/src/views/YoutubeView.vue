<template>
  <div>
    <div class="container-fluid px-4 mt-4">
      <!-- 헤더 -->
      <div class="d-flex align-items-center justify-content-between mb-4">
        <div>
          <h5 class="fw-bold mb-0"><i class="bi bi-youtube text-danger me-2"></i>유튜버 종목 분석</h5>
          <small class="text-muted">한국 주식 유튜버의 최근 영상에서 언급된 종목과 현재 시세</small>
        </div>
        <div class="d-flex gap-2 align-items-center">
          <span v-if="lastUpdated" class="text-muted small">
            <i class="bi bi-clock me-1"></i>{{ lastUpdated }}
          </span>
          <button class="btn btn-outline-primary btn-sm" @click="loadAnalysis(true)" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-1"></span>
            <i v-else class="bi bi-arrow-clockwise me-1"></i>새로고침
          </button>
        </div>
      </div>

      <!-- 로딩 -->
      <div v-if="loading && !analyses.length" class="row g-3">
        <div v-for="i in 3" :key="i" class="col-lg-4">
          <div class="card border-0 shadow-sm">
            <div class="card-body">
              <div class="placeholder-glow">
                <div class="placeholder col-6 mb-2" style="height:20px;border-radius:4px;"></div>
                <div class="placeholder col-12 mb-1" style="height:80px;border-radius:8px;"></div>
                <div class="placeholder col-12 mb-1" style="height:16px;border-radius:4px;"></div>
                <div class="placeholder col-8" style="height:16px;border-radius:4px;"></div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 채널 카드 그리드 -->
      <div v-else class="row g-4">
        <div v-for="a in analyses" :key="a.channel.id" class="col-lg-4">
          <div class="card border-0 shadow-sm h-100">

            <!-- 채널 헤더 -->
            <div class="card-header bg-white border-bottom py-3 px-3">
              <div class="d-flex align-items-center gap-3">
                <div class="channel-thumb">
                  <img v-if="a.channel.thumbnailUrl" :src="a.channel.thumbnailUrl"
                       :alt="a.channel.name" class="rounded-circle" width="48" height="48" />
                  <div v-else class="channel-placeholder rounded-circle d-flex align-items-center justify-content-center"
                       style="width:48px;height:48px;background:#ff4444;">
                    <i class="bi bi-youtube text-white fs-5"></i>
                  </div>
                </div>
                <div>
                  <div class="fw-bold">{{ a.channel.name }}</div>
                  <div class="text-muted small">{{ a.channel.description }}</div>
                </div>
              </div>
            </div>

            <!-- API 키 오류 -->
            <div v-if="a.error" class="card-body d-flex align-items-center justify-content-center">
              <div class="text-center text-muted py-3">
                <i class="bi bi-exclamation-circle text-warning fs-4 d-block mb-2"></i>
                <div class="small">{{ a.error }}</div>
                <a v-if="a.error.includes('API 키')"
                   href="https://console.cloud.google.com/" target="_blank"
                   class="btn btn-outline-warning btn-sm mt-2">
                  <i class="bi bi-gear me-1"></i>API 키 발급
                </a>
              </div>
            </div>

            <template v-else>
              <!-- 최근 영상 목록 -->
              <div class="card-body pb-0 px-3 pt-3">
                <div class="fw-semibold small text-muted mb-2">
                  <i class="bi bi-play-btn me-1"></i>최근 영상
                </div>
                <div v-if="!a.recentVideos.length" class="text-muted small text-center py-2">
                  영상 정보를 불러올 수 없습니다.
                </div>
                <div v-for="(v, idx) in a.recentVideos" :key="v.videoId"
                     class="video-item d-flex gap-2 mb-2"
                     :class="{ 'opacity-50': idx > 0 }">
                  <a :href="'https://www.youtube.com/watch?v=' + v.videoId"
                     target="_blank" class="text-decoration-none flex-shrink-0">
                    <img :src="v.thumbnailUrl" :alt="v.title"
                         class="rounded" style="width:80px;height:45px;object-fit:cover;"
                         onerror="this.style.display='none'" />
                  </a>
                  <div class="overflow-hidden">
                    <a :href="'https://www.youtube.com/watch?v=' + v.videoId"
                       target="_blank" class="text-decoration-none text-dark">
                      <div class="small fw-semibold video-title">{{ v.title }}</div>
                    </a>
                    <div class="text-muted" style="font-size:0.72rem;">{{ v.publishedAt }}</div>
                  </div>
                </div>
              </div>

              <!-- 구분선 -->
              <hr class="mx-3 my-2" />

              <!-- 언급 종목 -->
              <div class="px-3 pb-3">
                <div class="fw-semibold small text-muted mb-2">
                  <i class="bi bi-bar-chart-fill me-1 text-primary"></i>언급 종목
                  <span class="badge bg-primary ms-1">{{ a.mentionedStocks.length }}</span>
                </div>
                <div v-if="!a.mentionedStocks.length" class="text-muted small text-center py-2">
                  <i class="bi bi-info-circle me-1"></i>인식된 종목 없음
                </div>
                <table v-else class="table table-hover mb-0" style="font-size:0.82rem;">
                  <thead class="table-light">
                    <tr>
                      <th class="py-1 ps-2">종목명</th>
                      <th class="text-end py-1">현재가</th>
                      <th class="text-end py-1 pe-2">등락률</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="s in a.mentionedStocks" :key="s.code">
                      <td class="py-1 ps-2">
                        <span class="fw-semibold">{{ s.name }}</span>
                        <span class="text-muted ms-1" style="font-size:0.7rem;">{{ s.code }}</span>
                      </td>
                      <td class="text-end py-1">{{ fmtMoney(s.currentPrice) }}</td>
                      <td class="text-end py-1 pe-2">
                        <span class="badge"
                              :class="s.changeRate > 0 ? 'bg-danger' : s.changeRate < 0 ? 'bg-primary' : 'bg-secondary'">
                          {{ s.changeRate > 0 ? '+' : '' }}{{ s.changeRate.toFixed(2) }}%
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 안내 -->
      <div class="mt-4 p-3 rounded bg-white border text-muted small shadow-sm">
        <i class="bi bi-info-circle me-1"></i>
        영상 제목과 설명에서 종목명을 자동 인식합니다. 데이터는 30분간 캐시됩니다.
        YouTube Data API 키가 필요합니다 (<code>application.yml</code> → <code>youtube.api-key</code>).
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getYoutubeAnalysis, clearYoutubeCache } from '../api/index.js'

const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

const analyses = ref([])
const loading = ref(false)
const lastUpdated = ref('')

async function loadAnalysis(forceRefresh = false) {
  loading.value = true
  try {
    if (forceRefresh) await clearYoutubeCache()
    const res = await getYoutubeAnalysis()
    analyses.value = res.data
    const now = new Date()
    lastUpdated.value = `${now.getHours().toString().padStart(2,'0')}:${now.getMinutes().toString().padStart(2,'0')} 업데이트`
  } catch (e) {
    console.error('유튜브 분석 실패', e)
  } finally {
    loading.value = false
  }
}

function fmtMoney(v) {
  return Math.round(v || 0).toLocaleString('ko-KR')
}

onMounted(() => loadAnalysis())
</script>

<style scoped>
.video-title {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.3;
}
.channel-placeholder {
  background: linear-gradient(135deg, #ff4444, #cc0000);
}
</style>
