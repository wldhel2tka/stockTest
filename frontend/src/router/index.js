import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import PortfolioView from '../views/PortfolioView.vue'
import RecommendedView from '../views/RecommendedView.vue'

const routes = [
  { path: '/',           name: 'Login',     component: LoginView },
  { path: '/register',   name: 'Register',  component: RegisterView },
  { path: '/home',       name: 'Home',      component: HomeView,        meta: { requiresAuth: true } },
  { path: '/portfolio',  name: 'Portfolio', component: PortfolioView,   meta: { requiresAuth: true } },
  { path: '/recommend',  name: 'Recommend', component: RecommendedView, meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const user = sessionStorage.getItem('user')
  if (to.meta.requiresAuth && !user) {
    next('/')
  } else {
    next()
  }
})

export default router
