<template>
  <div class="min-vh-100 d-flex align-items-center justify-content-center bg-light py-4">
    <div class="card shadow-sm" style="width: 500px;">
      <div class="card-body p-4">
        <h4 class="card-title text-center mb-4 fw-bold">
          <i class="bi bi-person-plus-fill me-2 text-primary"></i>회원가입
        </h4>

        <div v-if="errorMsg" class="alert alert-danger py-2" role="alert">
          <i class="bi bi-exclamation-circle me-1"></i>{{ errorMsg }}
        </div>
        <div v-if="successMsg" class="alert alert-success py-2" role="alert">
          <i class="bi bi-check-circle me-1"></i>{{ successMsg }}
        </div>

        <form @submit.prevent="handleRegister" novalidate>
          <div class="mb-3">
            <label class="form-label fw-semibold">아이디 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person"></i></span>
              <input v-model="form.username" type="text" class="form-control"
                :class="{ 'is-invalid': errors.username }" placeholder="아이디를 입력하세요" />
              <div class="invalid-feedback">{{ errors.username }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">비밀번호 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock"></i></span>
              <input v-model="form.password" type="password" class="form-control"
                :class="{ 'is-invalid': errors.password }" placeholder="비밀번호를 입력하세요" />
              <div class="invalid-feedback">{{ errors.password }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">비밀번호 확인 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-lock-fill"></i></span>
              <input v-model="form.passwordConfirm" type="password" class="form-control"
                :class="{ 'is-invalid': errors.passwordConfirm }" placeholder="비밀번호를 다시 입력하세요" />
              <div class="invalid-feedback">{{ errors.passwordConfirm }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">이름 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-person-badge"></i></span>
              <input v-model="form.name" type="text" class="form-control"
                :class="{ 'is-invalid': errors.name }" placeholder="이름을 입력하세요" />
              <div class="invalid-feedback">{{ errors.name }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">이메일 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-envelope"></i></span>
              <input v-model="form.email" type="email" class="form-control"
                :class="{ 'is-invalid': errors.email }" placeholder="이메일을 입력하세요" />
              <div class="invalid-feedback">{{ errors.email }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">구분 <span class="text-danger">*</span></label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-briefcase"></i></span>
              <select v-model="form.userType" class="form-select" :class="{ 'is-invalid': errors.userType }">
                <option value="">선택하세요</option>
                <option value="CUSTOMER">고객사</option>
                <option value="DEVELOPER">개발사</option>
              </select>
              <div class="invalid-feedback">{{ errors.userType }}</div>
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-semibold">회사</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-building"></i></span>
              <input v-model="form.company" type="text" class="form-control" placeholder="회사명을 입력하세요" />
            </div>
          </div>

          <div class="mb-4">
            <label class="form-label fw-semibold">연락처</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi bi-telephone"></i></span>
              <input v-model="form.phone" type="tel" class="form-control" placeholder="010-0000-0000" />
            </div>
          </div>

          <button type="submit" class="btn btn-primary w-100 mb-3" :disabled="loading">
            <span v-if="loading" class="spinner-border spinner-border-sm me-2"></span>
            <i v-else class="bi bi-person-check me-2"></i>가입하기
          </button>
        </form>

        <p class="text-center mb-0 text-muted small">
          이미 계정이 있으신가요?
          <router-link to="/" class="text-primary fw-semibold">로그인</router-link>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/index.js'

const router = useRouter()

const form = ref({
  username: '', password: '', passwordConfirm: '',
  name: '', email: '', userType: '', company: '', phone: ''
})

const errors = ref({})
const errorMsg = ref('')
const successMsg = ref('')
const loading = ref(false)

function validate() {
  errors.value = {}
  if (!form.value.username) errors.value.username = '아이디를 입력하세요.'
  else if (form.value.username.length < 4) errors.value.username = '아이디는 4자 이상이어야 합니다.'

  if (!form.value.password) errors.value.password = '비밀번호를 입력하세요.'
  else if (form.value.password.length < 6) errors.value.password = '비밀번호는 6자 이상이어야 합니다.'

  if (!form.value.passwordConfirm) errors.value.passwordConfirm = '비밀번호 확인을 입력하세요.'
  else if (form.value.password !== form.value.passwordConfirm) errors.value.passwordConfirm = '비밀번호가 일치하지 않습니다.'

  if (!form.value.name) errors.value.name = '이름을 입력하세요.'

  if (!form.value.email) errors.value.email = '이메일을 입력하세요.'
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)) errors.value.email = '올바른 이메일 형식이 아닙니다.'

  if (!form.value.userType) errors.value.userType = '구분을 선택하세요.'

  return Object.keys(errors.value).length === 0
}

async function handleRegister() {
  errorMsg.value = ''
  successMsg.value = ''
  if (!validate()) return

  loading.value = true
  try {
    const { passwordConfirm, ...payload } = form.value
    await register(payload)
    successMsg.value = '회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.'
    setTimeout(() => router.push('/'), 1500)
  } catch (err) {
    errorMsg.value = err.response?.data?.message || '회원가입 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}
</script>
