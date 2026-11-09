import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { logoutRequest } from '@/api/auth'
import { queryClient } from '@/lib/queryClient'
import { useAuthStore } from '@/store/authStore'

/** Revokes the refresh token on the server (best effort), wipes local state and cached data, then goes to /login. */
export function useLogout() {
  const navigate = useNavigate()
  return useCallback(async () => {
    const { refreshToken, clear } = useAuthStore.getState()
    if (refreshToken) await logoutRequest(refreshToken).catch(() => undefined)
    clear()
    queryClient.clear()
    navigate('/login', { replace: true })
  }, [navigate])
}
