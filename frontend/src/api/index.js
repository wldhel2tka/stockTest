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

export default api
