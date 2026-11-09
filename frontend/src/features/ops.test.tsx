import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type MockAdapter from 'axios-mock-adapter'
import { isoDate } from '@/lib/dates'
import { ADMIN, mockHomeData, newMock, renderReady, signIn, STAFF } from '@/test/utils'

let mock: MockAdapter
const today = isoDate(new Date())
beforeEach(() => { mock = newMock() })
afterEach(() => mock.restore())
const body = (m: 'post' | 'put' | 'patch', url: string | RegExp) => JSON.parse(mock.history[m].find((r) => (typeof url === 'string' ? r.url === url : url.test(r.url ?? '')))!.data)

describe('staff counter', () => {
  const scanOk = { bookingId: 1, studentName: 'Asha Rao', rollNo: 'B423001', dietPref: 'VEG', date: today, mealType: 'LUNCH', method: 'QR', markedAt: '' }
  beforeEach(() => { signIn(STAFF); mockHomeData(mock); mock.onGet(/\/bookings\?/).reply(200, [{ id: 7, date: today, mealType: 'LUNCH', status: 'BOOKED', price: 60, studentName: 'Ben Das', rollNo: 'B423002' }]) })

  it('checks a student in from a pasted pass code and strips the THALI: prefix', async () => {
    mock.onPost('/attendance/scan').reply(200, scanOk)
    await renderReady('/app/serve')
    await userEvent.type(await screen.findByLabelText('Pass code'), 'THALI:abc-123')
    await userEvent.click(screen.getByRole('button', { name: 'Check in' }))
    expect(await screen.findByText('Asha Rao checked in')).toBeInTheDocument()
    expect(body('post', '/attendance/scan')).toEqual({ qrToken: 'abc-123' })
  })

  it('shows the server reason when a pass is refused', async () => {
    mock.onPost('/attendance/scan').reply(409, { code: 'ALREADY_SERVED', detail: 'This pass has already been used.' })
    await renderReady('/app/serve')
    await userEvent.type(await screen.findByLabelText('Pass code'), 'tok')
    await userEvent.click(screen.getByRole('button', { name: 'Check in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('already been used')
  })

  it('lists waiting students and marks one served manually', async () => {
    mock.onPost('/attendance/manual').reply(200, { ...scanOk, studentName: 'Ben Das' })
    await renderReady('/app/serve')
    const row = (await within(await screen.findByRole('list', { name: 'Waiting students' })).findAllByRole('listitem'))[0]!
    expect(row).toHaveTextContent('Ben Das')
    await userEvent.click(within(row).getByRole('button', { name: 'Mark served' }))
    expect(await screen.findByText('Ben Das checked in')).toBeInTheDocument()
    expect(body('post', '/attendance/manual')).toEqual({ bookingId: 7 })
  })
})

describe('menu editing (staff)', () => {
  it('picks dishes and publishes a menu', async () => {
    signIn(STAFF)
    mock.onGet(/\/menus\?/).reply(200, [])
    mock.onGet('/menu-items').reply(200, [{ id: 1, name: 'Dal Tadka', category: 'MAIN', veg: true, active: true }, { id: 2, name: 'Kheer', category: 'DESSERT', veg: true, active: true }])
    mock.onPut(/\/menus\//).reply(200, {})
    await renderReady('/app/menu-today')
    await userEvent.click((await screen.findAllByRole('button', { name: /Edit Lunch on/ }))[0]!)
    await userEvent.click(await screen.findByLabelText(/Dal Tadka/))
    await userEvent.click(screen.getByRole('button', { name: 'Save & publish' }))
    expect(JSON.parse(mock.history.put[0]!.data)).toEqual({ itemIds: [1], publish: true })
    expect(mock.history.put[0]!.url).toBe(`/menus/${today}/LUNCH`)
  })
})

describe('complaints queue', () => {
  const c = { id: 3, category: 'SERVICE', title: 'Long queue', description: 'Slow', priority: 'HIGH', status: 'OPEN', studentName: 'Asha Rao', rollNo: 'B423001', assignedToName: null, createdAt: '', updatedAt: '', updates: [] }
  it('staff reply and change status', async () => {
    signIn(STAFF)
    mock.onGet(/\/complaints\?/).reply(200, { content: [c], totalElements: 1 })
    mock.onGet('/complaints/3').reply(200, c)
    mock.onPost('/complaints/3/updates').reply(201, c)
    await renderReady('/app/complaints')
    await userEvent.click(await screen.findByText('Long queue'))
    await userEvent.type(await screen.findByLabelText('Reply'), 'On it')
    await userEvent.selectOptions(screen.getByLabelText('New status'), 'IN_PROGRESS')
    await userEvent.click(screen.getByRole('button', { name: 'Send' }))
    expect(body('post', '/complaints/3/updates')).toEqual({ message: 'On it', newStatus: 'IN_PROGRESS' })
    expect(screen.queryByLabelText('Assign to')).not.toBeInTheDocument()
  })
  it('admins can also assign', async () => {
    signIn(ADMIN)
    mock.onGet(/\/complaints\?/).reply(200, { content: [c], totalElements: 1 })
    mock.onGet('/complaints/3').reply(200, c)
    mock.onGet(/\/users/).reply(200, { content: [{ id: 3, name: 'Ramesh', role: 'STAFF', active: true }] })
    mock.onPatch('/complaints/3/assign').reply(200, {})
    await renderReady('/app/complaints')
    await userEvent.click(await screen.findByText('Long queue'))
    await userEvent.selectOptions(await screen.findByLabelText('Assign to'), '3')
    expect(body('patch', '/complaints/3/assign')).toEqual({ staffId: 3 })
  })
})

describe('admin screens', () => {
  beforeEach(() => signIn(ADMIN))

  it('students: approves a pending registration', async () => {
    mock.onGet(/\/students/).reply(200, { content: [{ userId: 9, name: 'Priya', email: 'b424777@iiit-bh.ac.in', rollNo: 'B424777', hostelBlock: 'G1', roomNo: '2', approvalStatus: 'PENDING', active: true }] })
    mock.onPatch('/students/9/approval').reply(200, {})
    await renderReady('/app/students')
    await userEvent.click(await screen.findByRole('button', { name: 'Approve' }))
    expect(body('patch', '/students/9/approval')).toEqual({ status: 'APPROVED' })
    expect(mock.history.get[0]!.url).toContain('status=PENDING')
  })

  it('billing: generates a month and records cash for an unpaid bill', async () => {
    mock.onGet(/\/bills\?/).reply(200, { content: [{ id: 4, studentName: 'Asha', rollNo: 'B1', mealCount: 5, total: 535, status: 'UNPAID' }, { id: 5, studentName: 'Ben', rollNo: 'B2', mealCount: 0, total: 300, status: 'PAID' }] })
    mock.onPost(/\/bills\/generate/).reply(200, { created: 2, updated: 0, skippedPaid: 0 })
    mock.onPost('/payments/cash').reply(201, {})
    await renderReady('/app/billing')
    await userEvent.click(await screen.findByRole('button', { name: /Generate bills/ }))
    expect(mock.history.post[0]!.url).toMatch(/^\/bills\/generate\?month=\d{4}-\d{2}$/)
    expect(screen.getAllByRole('button', { name: 'Record cash' })).toHaveLength(1)   // the paid bill has none
    await userEvent.click(screen.getByRole('button', { name: 'Record cash' }))
    expect(body('post', '/payments/cash')).toEqual({ billId: 4, amount: 535 })
  })

  it('meal settings: saves a changed price', async () => {
    mock.onGet('/meal-slots').reply(200, [{ mealType: 'LUNCH', startTime: '12:30:00', endTime: '14:30:00', cutoffMinutes: 240, price: 60, active: true }])
    mock.onPut('/meal-slots/LUNCH').reply(200, {})
    await renderReady('/app/meal-settings')
    const price = await screen.findByLabelText('Price (₹)')
    await userEvent.clear(price); await userEvent.type(price, '75')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(body('put', '/meal-slots/LUNCH')).toMatchObject({ price: 75, startTime: '12:30', cutoffMinutes: 240 })
  })

  it('dishes: creates a dish; staff: validates and creates an account', async () => {
    mock.onGet('/menu-items').reply(200, [])
    mock.onPost('/menu-items').reply(201, {})
    await renderReady('/app/dishes')
    await userEvent.click(await screen.findByRole('button', { name: /New dish/ }))
    await userEvent.type(screen.getByLabelText('Name'), 'Poha')
    await userEvent.click(screen.getByRole('button', { name: 'Save' }))
    expect(body('post', '/menu-items')).toMatchObject({ name: 'Poha', category: 'MAIN', veg: true })
  })

  it('staff page: the create button stays disabled until the password is strong', async () => {
    mock.onGet(/role=STAFF/).reply(200, { content: [{ id: 3, name: 'Ramesh', email: 'r@x.in', role: 'STAFF', active: true }] })
    mock.onGet(/role=ADMIN/).reply(200, { content: [] })
    mock.onPost('/users').reply(201, {})
    await renderReady('/app/staff')
    await userEvent.click(await screen.findByRole('button', { name: /Add staff/ }))
    await userEvent.type(screen.getByLabelText('Full name'), 'New Person')
    await userEvent.type(screen.getByLabelText('Email'), 'new@thali.local')
    await userEvent.type(screen.getByLabelText('Temporary password'), 'weak')
    expect(screen.getByRole('button', { name: 'Create account' })).toBeDisabled()
    await userEvent.clear(screen.getByLabelText('Temporary password')); await userEvent.type(screen.getByLabelText('Temporary password'), 'Staff12345')
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(body('post', '/users')).toMatchObject({ role: 'STAFF', email: 'new@thali.local' })
  })

  it('announcements: posts a pinned notice', async () => {
    mock.onGet('/announcements').reply(200, [])
    mock.onPost('/announcements').reply(201, {})
    await renderReady('/app/announcements')
    await userEvent.type(await screen.findByLabelText('Title'), 'Diwali')
    await userEvent.type(screen.getByLabelText('Message'), 'Dinner only')
    await userEvent.click(screen.getByLabelText(/Pin to the top/))
    await userEvent.click(screen.getByRole('button', { name: 'Post announcement' }))
    expect(body('post', '/announcements')).toEqual({ title: 'Diwali', body: 'Dinner only', pinned: true })
  })

  it('feedback: shows the average, distribution and dish rankings', async () => {
    mock.onGet('/feedback/summary').reply(200, { averageRating: 3.86, totalRatings: 10, byMeal: [], distribution: { 1: 0, 2: 1, 3: 2, 4: 4, 5: 3 } })
    mock.onGet(/\/feedback\?/).reply(200, { content: [{ id: 1, date: today, mealType: 'LUNCH', rating: 5, comment: 'Great', studentName: 'Asha' }] })
    mock.onGet(/top-dishes\?order=best/).reply(200, [{ dishId: 1, name: 'Gulab Jamun', averageRating: 4.7, ratings: 20 }])
    mock.onGet(/top-dishes\?order=worst/).reply(200, [{ dishId: 2, name: 'Bhindi Fry', averageRating: 2.9, ratings: 9 }])
    await renderReady('/app/feedback')
    expect(await screen.findByText('3.86')).toBeInTheDocument()
    expect(await screen.findByText('Gulab Jamun')).toBeInTheDocument()
    expect(await screen.findByText('Bhindi Fry')).toBeInTheDocument()
    expect(screen.getByText('“Great”')).toBeInTheDocument()
  })

  it('menu builder + attendance render for admins', async () => {
    mock.onGet(/\/menus\?/).reply(200, [])
    mock.onGet('/menu-items').reply(200, [])
    mockHomeData(mock)
    const { unmount } = await renderReady('/app/menu-builder')
    expect(await screen.findAllByRole('button', { name: /Edit Dinner on/ })).toHaveLength(7)
    unmount()
    await renderReady('/app/attendance')
    expect(await screen.findByRole('button', { name: /Download attendance CSV/ })).toBeInTheDocument()
  })
})
