import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { AuthResponse, Role, User } from '@/types/api'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: User | null
  /** true when the session ended because the refresh token was rejected (shows a notice on /login) */
  sessionExpired: boolean
  setSession: (auth: AuthResponse) => void
  setUser: (user: User) => void
  clear: (expired?: boolean) => void
}

/**
 * Tokens live in localStorage so a refresh keeps you signed in. Trade-off: any XSS could read them; the app renders no
 * user HTML and the access token lives only 15 minutes, but a hardened deployment would move the refresh token to an
 * httpOnly cookie.
 */
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      sessionExpired: false,
      setSession: (a) => set({ accessToken: a.accessToken, refreshToken: a.refreshToken, user: a.user, sessionExpired: false }),
      setUser: (user) => set({ user }),
      clear: (expired = false) => set({ accessToken: null, refreshToken: null, user: null, sessionExpired: expired }),
    }),
    {
      name: 'thali-auth',
      partialize: (s) => ({ accessToken: s.accessToken, refreshToken: s.refreshToken, user: s.user }),
    },
  ),
)

export const ROLE_HOME: Record<Role, string> = {
  STUDENT: '/app/home',
  STAFF: '/app/serve',
  ADMIN: '/app/dashboard',
}
