import { get, post } from './client'
import type { AuthResponse, OtpPurpose, OtpVerifyResponse, User } from '@/types/api'

const open = { skipAuth: true } as const   // these endpoints are public; never send (or refresh) a token for them

export const requestOtp = (email: string, purpose: OtpPurpose) => post<{ message: string }>('/auth/otp/request', { email, purpose }, open)

export const verifyOtp = (email: string, purpose: OtpPurpose, code: string) =>
  post<OtpVerifyResponse>('/auth/otp/verify', { email, purpose, code }, open)

export interface RegisterPayload {
  registrationToken: string
  name: string
  phone: string
  hostelBlock: string
  roomNo: string
  dietPref: string
  password: string
}

export const registerStudent = (payload: RegisterPayload) => post<User>('/auth/register', payload, open)

export const loginWithPassword = (email: string, password: string) => post<AuthResponse>('/auth/login', { email, password }, open)

export const resetPassword = (email: string, code: string, newPassword: string) =>
  post<{ message: string }>('/auth/password/reset', { email, code, newPassword }, open)

export const logoutRequest = (refreshToken: string) => post<{ message: string }>('/auth/logout', { refreshToken }, open)

export const fetchMe = () => get<User>('/auth/me')
