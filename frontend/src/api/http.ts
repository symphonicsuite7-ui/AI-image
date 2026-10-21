import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types/image'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 210_000,
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body.code !== 200) {
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const message = error.response?.data?.message || error.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export default http
