import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { NAV } from '@/components/layout/nav'
import { ROLE_HOME, useAuthStore } from '@/store/authStore'
import type { Role } from '@/types/api'

/** Must be signed in (and, if `roles` is given, have one of those roles). Otherwise bounce to /login or /403. */
export function ProtectedRoute({ roles }: { roles?: Role[] }) {
  const user = useAuthStore((s) => s.user)
  const location = useLocation()
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (roles && !roles.includes(user.role)) return <Navigate to="/403" replace />
  return <Outlet />
}

/** Where a freshly signed-in user lands: the page they were bounced from (if their role may open it), else their own home. */
export function landingFor(role: Role, from?: string): string {
  return from && NAV[role].some((n) => n.to === from) ? from : ROLE_HOME[role]
}

/**
 * Login/register pages: signed-in users are redirected away. This guard (not the login form) owns the redirect, because storing the
 * session re-renders it synchronously, before any navigate() call in the form's submit handler could run.
 */
export function PublicOnlyRoute() {
  const user = useAuthStore((s) => s.user)
  const from = (useLocation().state as { from?: string } | null)?.from
  if (user) return <Navigate to={landingFor(user.role, from)} replace />
  return <Outlet />
}

/** /app -> the signed-in user's own landing page. */
export function RoleRedirect() {
  const user = useAuthStore((s) => s.user)
  return <Navigate to={user ? ROLE_HOME[user.role] : '/login'} replace />
}
