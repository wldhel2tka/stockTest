<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light">
    <div class="card shadow-sm" style="width: 400px;">
      <div class="card-body p-4">
        <h4 class="card-title text-center mb-4 fw-bold">
          <i class="bi bi-person-circle me-2 text-primary"></i>로그인
        </h4>

        <div v-if="errorMsg" class="alert alert-danger py-2" role="alert">
          <i class="bi bi-exclamation-circle me-1"></i>{{ errorMsg }}
        </div>

        <form @submit.prevent="handleLogin">
          <div class="mb-3">
            <label class="form-label fw-semibold">아이디</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person"></i></span>
              <input
                v-model="form.username"
                type="text"
                class="form-control"
                placeholder="아이디를 입력하세요"
                required
              />
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label fw-semibold">비밀번호</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock"></i></span>
              <input
                v-model="form.password"
                type="password"
                class="form-control"
                placeholder="비밀번호를 입력하세요"
                required
              />
            </div>
          </div>

          <button type="submit" class="btn btn-primary w-100 mb-3" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-box-arrow-in-right me-2"></i>로그인
          </button>
        </form>

        <hr />

        <p class="text-center mb-0 text-muted small">
          아직 계정이 없으신가요?
          <router-link to="/register" class="text-primary fw-semibold">회원가입</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/index.js'

const router = useRouter()
const form = ref({ username: '', password: '' })
const errorMsg = ref('')
const loading = ref(false)

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    const res = await login(form.value.username, form.value.password)
    sessionStorage.setItem('user', JSON.stringify(res.data.user))
    router.push('/home')
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '로그인 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
