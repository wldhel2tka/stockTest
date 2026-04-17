<template>
  <div>
    <nav class="navbar navbar-expand-lg navbar-dark bg-primary">
      <div class="container">
        <span class="navbar-brand fw-bold">
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

    <div class="container mt-5">
      <div class="text-center">
        <h2 class="fw-bold">환영합니다, {{ user?.name }} 님!</h2>
        <p class="text-muted">로그인에 성공하였습니다.</p>
      </div>
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
