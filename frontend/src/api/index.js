import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

export const login = (username, password) =>
  api.post('/auth/login', { username, password })

export const register = (userData) =>
  api.post('/auth/register', userData)

export const searchStocks = (query) =>
  api.get('/stock/search', { params: { query } })

export const getStockPrice = (code) =>
  api.get('/stock/price', { params: { code } })

export const getStockChart = (code, period, startDate, endDate) =>
  api.get('/stock/chart', { params: { code, period, startDate, endDate } })

export const getMinuteChart = (code, type) =>
  api.get('/stock/chart/minute', { params: { code, type } })

// 모의투자
export const getAccount    = (userId) => api.get('/mock/account', { params: { userId } })
export const getPortfolio  = (userId) => api.get('/mock/portfolio', { params: { userId } })
export const buyStock      = (userId, code, quantity) => api.post('/mock/buy',  { userId, code, quantity })
export const sellStock     = (userId, code, quantity) => api.post('/mock/sell', { userId, code, quantity })
export const getTransactions = (userId) => api.get('/mock/transactions', { params: { userId } })
export const resetAccount  = (userId) => api.post('/mock/reset', { userId })

// 추천 종목
export const getTopVolume = (market = 'J', minPrice = 0, maxPrice = 0, minVolume = 0) =>
  api.get('/recommend/volume',  { params: { market, minPrice, maxPrice, minVolume } })
export const getGainers   = (market = 'J', minPrice = 0, maxPrice = 0, minVolume = 0) =>
  api.get('/recommend/gainers', { params: { market, minPrice, maxPrice, minVolume } })
export const getLosers    = (market = 'J', minPrice = 0, maxPrice = 0, minVolume = 0) =>
  api.get('/recommend/losers',  { params: { market, minPrice, maxPrice, minVolume } })

// 유튜버 분석
export const getYoutubeAnalysis = () => api.get('/youtube/analysis')
export const clearYoutubeCache  = () => api.post('/youtube/cache/clear')

// 뉴스 분석
export const getNewsAnalysis = (refresh = false) => api.get('/news/analysis', { params: { refresh } })

export default api
