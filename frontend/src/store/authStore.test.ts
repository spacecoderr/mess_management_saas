import { authFor, STUDENT } from '@/test/utils'
import { ROLE_HOME, useAuthStore } from './authStore'

describe('authStore', () => {
  beforeEach(() => useAuthStore.getState().clear())

  it('starts signed out', () => {
    const s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.accessToken).toBeNull()
  })

  it('stores a session and persists tokens + user (not the transient flag)', () => {
    useAuthStore.getState().clear(true)
    useAuthStore.getState().setSession(authFor(STUDENT))
    const s = useAuthStore.getState()
    expect(s.user?.rollNo).toBe('Z900002')
    expect(s.sessionExpired).toBe(false)   // logging in clears the "expired" notice

    const saved = JSON.parse(localStorage.getItem('thali-auth')!).state
    expect(saved.accessToken).toBe('access-a')
    expect(saved.refreshToken).toBe('refresh-a')
    expect(saved.user.email).toBe(STUDENT.email)
    expect('sessionExpired' in saved).toBe(false)
  })

  it('clear() wipes everything and can flag an expired session', () => {
    useAuthStore.getState().setSession(authFor(STUDENT))
    useAuthStore.getState().clear(true)
    const s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.refreshToken).toBeNull()
    expect(s.sessionExpired).toBe(true)
    expect(JSON.parse(localStorage.getItem('thali-auth')!).state.accessToken).toBeNull()
  })

  it('maps each role to its own home page', () => {
    expect(ROLE_HOME).toEqual({ STUDENT: '/app/home', STAFF: '/app/serve', ADMIN: '/app/dashboard' })
  })
})
