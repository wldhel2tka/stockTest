import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

export const login = (username, password) =>
  api.post('/auth/login', { username, password })

export const register = (userData) =>
  api.post('/auth/register', userData)

export default api
