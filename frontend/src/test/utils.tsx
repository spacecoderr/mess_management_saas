import { render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import MockAdapter from 'axios-mock-adapter'
import type { ReactNode } from 'react'
import { http } from '@/api/client'
import { AppRoutes } from '@/routes'
import { useAuthStore } from '@/store/authStore'
import type { AuthResponse, User } from '@/types/api'

export const STUDENT: User = {
  id: 7, name: 'Ananya Behera', email: 'z900002@iiit-bh.ac.in', role: 'STUDENT', phone: '9876543210', emailVerified: true,
  approvalStatus: 'APPROVED', rollNo: 'Z900002', batchYear: 2023, hostelBlock: 'Girls Hostel A', roomNo: '393', dietPref: 'VEG',
}
export const STAFF: User = { ...STUDENT, id: 3, name: 'Ramesh Behera', email: 'staff1@thali.local', role: 'STAFF', approvalStatus: null, rollNo: null, batchYear: null, hostelBlock: null, roomNo: null, dietPref: null }
export const ADMIN: User = { ...STAFF, id: 1, name: 'Mess Admin', email: 'admin@thali.local', role: 'ADMIN' }

export const authFor = (user: User, tag = 'a'): AuthResponse => ({ accessToken: `access-${tag}`, refreshToken: `refresh-${tag}`, tokenType: 'Bearer', expiresIn: 900, user })

export const signIn = (user: User) => useAuthStore.getState().setSession(authFor(user))
export const signOut = () => useAuthStore.setState({ accessToken: null, refreshToken: null, user: null, sessionExpired: false })

export const newMock = () => new MockAdapter(http, { onNoMatch: 'throwException' })

/** Canned responses for every endpoint the three home pages read. */
export function mockHomeData(mock: MockAdapter) {
  mock.onGet('/menus/today').reply(200, [
    { id: 1, date: '2026-11-09', mealType: 'LUNCH', published: true, startTime: '12:30:00', endTime: '14:30:00', price: 60, bookingClosesAt: '2026-11-09T03:00:00Z', bookingOpen: false,
      items: [{ id: 1, name: 'Dal Tadka', description: null, category: 'MAIN', veg: true, imageUrl: null, active: true }, { id: 2, name: 'Chicken Curry', description: null, category: 'MAIN', veg: false, imageUrl: null, active: true }] },
    { id: 2, date: '2026-11-09', mealType: 'DINNER', published: true, startTime: '20:00:00', endTime: '21:30:00', price: 60, bookingClosesAt: '2099-01-01T10:30:00Z', bookingOpen: true, items: [] },
  ])
  mock.onGet('/announcements').reply(200, [
    { id: 1, title: 'Welcome to Thali!', body: 'Book before the cutoff.', pinned: true, author: 'Mess Admin', createdAt: '2026-11-01T00:00:00Z' },
  ])
  mock.onGet('/bills/me').reply(200, [{ id: 5, month: '2026-10', mealCount: 74, mealCharges: 3505, fixedCharges: 300, total: 3805, status: 'OVERDUE', dueDate: '2026-11-24' }])
  mock.onGet('/analytics/overview').reply(200, {
    approvedStudents: 25, pendingApprovals: 2, todayBookings: 44, todayServed: 25, revenueThisMonth: 47104, outstanding: 70677, overdueBills: 10, averageRating30d: 3.86, openComplaints: 6,
  })
  mock.onGet(/\/analytics\/(bookings-trend|revenue)/).reply(200, [])
  mock.onGet('/analytics/wastage').reply(200, { noShowRatePercent: 7.12, noShow: 126, lostValue: 6165, byMeal: [] })
  mock.onGet('/attendance/live').reply(200, [
    { date: '2026-11-09', mealType: 'BREAKFAST', menuId: 1, booked: 11, served: 9, remaining: 0, noShow: 2, cancelled: 1 },
    { date: '2026-11-09', mealType: 'LUNCH', menuId: 2, booked: 20, served: 18, remaining: 0, noShow: 2, cancelled: 0 },
    { date: '2026-11-09', mealType: 'SNACKS', menuId: null, booked: 0, served: 0, remaining: 0, noShow: 0, cancelled: 0 },
    { date: '2026-11-09', mealType: 'DINNER', menuId: 3, booked: 19, served: 2, remaining: 17, noShow: 0, cancelled: 2 },
  ])
}

export function renderRoute(path: string, extra?: ReactNode) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppRoutes />
        {extra}
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

/** Like renderRoute, but waits until the lazy page chunk has loaded (the Suspense "Loading" skeleton is gone). */
export async function renderReady(path: string, extra?: ReactNode) {
  const result = renderRoute(path, extra)
  await waitFor(() => expect(screen.queryByRole('status', { name: 'Loading' })).not.toBeInTheDocument())
  return result
}
