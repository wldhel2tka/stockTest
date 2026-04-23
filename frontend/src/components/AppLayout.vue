<template>
  <div class="d-flex min-vh-100">
    <!-- 좌측 사이드바 -->
    <nav class="d-flex flex-column bg-primary shadow" style="width:220px;min-height:100vh;position:fixed;left:0;top:0;z-index:100;">
      <!-- 브랜드 -->
      <div class="px-3 py-4 border-bottom border-white border-opacity-25">
        <span class="fw-bold fs-5 text-white">
          <i class="bi bi-graph-up-arrow me-2"></i>StockTest
        </span>
      </div>

      <!-- 메뉴 -->
      <div class="flex-grow-1 py-3">
        <router-link to="/home" class="sidebar-link" :class="{ active: $route.path === '/home' }">
          <i class="bi bi-search"></i><span>주식 검색</span>
        </router-link>
        <router-link to="/portfolio" class="sidebar-link" :class="{ active: $route.path === '/portfolio' }">
          <i class="bi bi-briefcase"></i><span>포트폴리오</span>
        </router-link>
        <router-link to="/youtube" class="sidebar-link" :class="{ active: $route.path === '/youtube' }">
          <i class="bi bi-youtube"></i><span>유튜버 분석</span>
        </router-link>
        <router-link to="/news" class="sidebar-link" :class="{ active: $route.path === '/news' }">
          <i class="bi bi-newspaper"></i><span>뉴스 분석</span>
        </router-link>
        <router-link to="/ipo" class="sidebar-link" :class="{ active: $route.path === '/ipo' }">
          <i class="bi bi-calendar-event"></i><span>공모주 일정</span>
        </router-link>
        <div class="mx-3 my-2 border-top border-white border-opacity-25"></div>
        <router-link to="/real" class="sidebar-link" :class="{ active: $route.path === '/real' }">
          <i class="bi bi-bank"></i><span>실계좌 주문</span>
        </router-link>
      </div>

      <!-- 하단: 사용자 + 로그아웃 -->
      <div class="px-3 py-3 border-top border-white border-opacity-25">
        <div class="text-white small mb-2 opacity-75">
          <i class="bi bi-person-circle me-1"></i>{{ user?.name }} 님
        </div>
        <button class="btn btn-outline-light btn-sm w-100" @click="handleLogout">
          <i class="bi bi-box-arrow-right me-1"></i>로그아웃
        </button>
      </div>
    </nav>

    <!-- 메인 콘텐츠 -->
    <div class="flex-grow-1 bg-light" style="margin-left:220px;min-height:100vh;">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

function handleLogout() {
  sessionStorage.removeItem('user')
  router.push('/')
}
</script>

<style scoped>
.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  margin: 2px 10px;
  border-radius: 8px;
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 0.9rem;
  transition: background 0.15s, color 0.15s;
}
.sidebar-link:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}
.sidebar-link.active {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 600;
}
</style>
