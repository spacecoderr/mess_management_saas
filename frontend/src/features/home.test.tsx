import { screen, within } from '@testing-library/react'
import { ADMIN, mockHomeData, newMock, renderRoute, signIn, STAFF, STUDENT } from '@/test/utils'
import type MockAdapter from 'axios-mock-adapter'

let mock: MockAdapter
beforeEach(() => (mock = newMock()))
afterEach(() => mock.restore())

describe('student home', () => {
  it('shows a greeting, today\'s meals with dishes, the latest bill and the notice board', async () => {
    mockHomeData(mock)
    signIn(STUDENT)
    renderRoute('/app/home')

    expect(await screen.findByRole('heading', { name: /Good (morning|afternoon|evening), Ananya/ })).toBeInTheDocument()
    expect(screen.getByText(/Z900002/)).toBeInTheDocument()

    const lunch = await screen.findByRole('article', { name: 'Lunch' })
    expect(within(lunch).getByText('Dal Tadka')).toBeInTheDocument()
    expect(within(lunch).getByText(/12:30 PM – 2:30 PM · ₹60/)).toBeInTheDocument()
    expect(within(lunch).getByText('Booking closed')).toBeInTheDocument()
    expect(within(lunch).getByLabelText('non-vegetarian')).toBeInTheDocument()
    expect(within(screen.getByRole('article', { name: 'Dinner' })).getByText(/Open till/)).toBeInTheDocument()

    expect(await screen.findByText('₹3,805')).toBeInTheDocument()
    expect(screen.getByText('overdue')).toBeInTheDocument()
    expect(screen.getByText('Welcome to Thali!')).toBeInTheDocument()
  })

  it('has friendly empty states when nothing is published, billed or announced', async () => {
    mock.onGet('/menus/today').reply(200, [])
    mock.onGet('/bills/me').reply(200, [])
    mock.onGet('/announcements').reply(200, [])
    signIn(STUDENT)
    renderRoute('/app/home')
    expect(await screen.findByText('No menu published yet')).toBeInTheDocument()
    expect(screen.getByText(/No bills yet/)).toBeInTheDocument()
    expect(screen.getByText('Nothing new right now.')).toBeInTheDocument()
  })

  it('keeps the page usable when one panel fails to load', async () => {
    mock.onGet('/menus/today').reply(200, [])
    mock.onGet('/bills/me').reply(500, { code: 'INTERNAL_ERROR', detail: 'boom' })
    mock.onGet('/announcements').reply(200, [])
    signIn(STUDENT)
    renderRoute('/app/home')
    expect(await screen.findByText('No menu published yet')).toBeInTheDocument()   // the rest still renders
    expect(screen.getByRole('heading', { name: 'Latest bill' })).toBeInTheDocument()
  })
})

describe('staff serve counter', () => {
  it('shows live served / booked counts per meal', async () => {
    mockHomeData(mock)
    signIn(STAFF)
    renderRoute('/app/serve')

    expect(await screen.findByText('29 of 50 served today')).toBeInTheDocument()
    expect(screen.getByRole('img', { name: '9 of 11 served' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: '18 of 20 served' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: '2 of 19 served' })).toBeInTheDocument()
    const dinner = screen.getByLabelText('Dinner')
    expect(within(dinner).getByText('Waiting').nextSibling).toHaveTextContent('17')
    expect(screen.getByText('No menu today')).toBeInTheDocument()   // snacks has no menu
  })

  it('explains when no meals are scheduled', async () => {
    mock.onGet('/attendance/live').reply(200, ['BREAKFAST', 'LUNCH', 'SNACKS', 'DINNER'].map((m) => ({ date: '2026-11-09', mealType: m, menuId: null, booked: 0, served: 0, remaining: 0, noShow: 0, cancelled: 0 })))
    signIn(STAFF)
    renderRoute('/app/serve')
    expect(await screen.findByText('No meals scheduled today')).toBeInTheDocument()
  })
})

describe('admin dashboard', () => {
  it('shows the KPI tiles with formatted rupee amounts and a pending-approvals banner', async () => {
    mockHomeData(mock)
    signIn(ADMIN)
    renderRoute('/app/dashboard')

    expect(await screen.findByText('₹47,104')).toBeInTheDocument()
    expect(screen.getByText('₹70,677')).toBeInTheDocument()
    expect(screen.getAllByText('25')).toHaveLength(2)   // approved students and served-today are both 25
    expect(screen.getByText('3.86')).toBeInTheDocument()
    expect(screen.getByText('Revenue this month')).toBeInTheDocument()
    expect(screen.getByText(/2 students are waiting for approval/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Review' })).toHaveAttribute('href', '/app/students')
  })

  it('shows "No ratings yet" instead of a fake zero, and no banner when nobody is pending', async () => {
    mock.onGet('/analytics/overview').reply(200, { approvedStudents: 5, pendingApprovals: 0, todayBookings: 0, todayServed: 0, revenueThisMonth: 0, outstanding: 0, overdueBills: 0, averageRating30d: null, openComplaints: 0 })
    signIn(ADMIN)
    renderRoute('/app/dashboard')
    expect(await screen.findByText('No ratings yet')).toBeInTheDocument()
    expect(screen.queryByText(/waiting for approval/)).not.toBeInTheDocument()
  })

  it('shows the error when the overview cannot be loaded', async () => {
    mock.onGet('/analytics/overview').reply(500, { code: 'INTERNAL_ERROR', detail: 'Something went wrong on our side.' })
    signIn(ADMIN)
    renderRoute('/app/dashboard')
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong on our side.')
  })
})
