import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import RegisterView from '../views/RegisterView.vue'
import HomeView from '../views/HomeView.vue'
import PortfolioView from '../views/PortfolioView.vue'
import YoutubeView from '../views/YoutubeView.vue'
import NewsView from '../views/NewsView.vue'
import IpoView from '../views/IpoView.vue'
import RealTradingView from '../views/RealTradingView.vue'

const routes = [
  { path: '/',           name: 'Login',     component: LoginView },
  { path: '/register',   name: 'Register',  component: RegisterView },
  { path: '/home',       name: 'Home',      component: HomeView,      meta: { requiresAuth: true } },
  { path: '/portfolio',  name: 'Portfolio', component: PortfolioView, meta: { requiresAuth: true } },
  { path: '/youtube',    name: 'Youtube',   component: YoutubeView,   meta: { requiresAuth: true } },
  { path: '/news',       name: 'News',      component: NewsView,      meta: { requiresAuth: true } },
  { path: '/ipo',        name: 'Ipo',       component: IpoView,       meta: { requiresAuth: true } },
  { path: '/real',      name: 'Real',      component: RealTradingView, meta: { requiresAuth: true } },
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
