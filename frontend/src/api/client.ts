import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { toApiError } from '@/lib/errors'
import { useAuthStore } from '@/store/authStore'
import type { AuthResponse } from '@/types/api'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** Do not attach the Authorization header and do not try to refresh on 401 (login, refresh, OTP...). */
    skipAuth?: boolean
    _retried?: boolean
  }
}

// Dev: empty base -> Vite proxies /api to the backend. Production: VITE_API_URL points at the deployed API.
const base = ((import.meta.env.VITE_API_URL as string | undefined) ?? '').replace(/\/$/, '')

/** 45 s timeout: a sleeping free-tier server can take 30-50 s for its first response. */
export const http = axios.create({ baseURL: `${base}/api/v1`, timeout: 45_000, headers: { 'Content-Type': 'application/json' } })

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken
  if (token && !config.skipAuth) config.headers.set('Authorization', `Bearer ${token}`)
  return config
})

let refreshing: Promise<string> | null = null

/** One refresh at a time: if five requests hit 401 together they all wait for the same refresh call. */
function refreshAccessToken(): Promise<string> {
  const { refreshToken } = useAuthStore.getState()
  if (!refreshToken) return Promise.reject(new Error('no refresh token'))
  refreshing ??= http
    .post<AuthResponse>('/auth/refresh', { refreshToken }, { skipAuth: true })
    .then(({ data }) => {
      useAuthStore.getState().setSession(data)
      return data.accessToken
    })
    .catch((e) => {
      useAuthStore.getState().clear(true)
      throw e
    })
    .finally(() => {
      refreshing = null
    })
  return refreshing
}

http.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as (AxiosRequestConfig & { _retried?: boolean }) | undefined
    if (error.response?.status === 401 && original && !original._retried && !original.skipAuth && useAuthStore.getState().refreshToken) {
      original._retried = true
      try {
        const token = await refreshAccessToken()
        original.headers = { ...(original.headers ?? {}), Authorization: `Bearer ${token}` }
        return await http(original)
      } catch (refreshError) {
        throw toApiError(refreshError)
      }
    }
    throw toApiError(error)
  },
)

/** Unwraps `.data` so API modules stay one-liners. */
export const get = <T>(url: string, config?: AxiosRequestConfig) => http.get<T>(url, config).then((r) => r.data)
export const post = <T>(url: string, body?: unknown, config?: AxiosRequestConfig) => http.post<T>(url, body, config).then((r) => r.data)
