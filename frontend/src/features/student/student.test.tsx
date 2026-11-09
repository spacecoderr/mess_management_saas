import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type MockAdapter from 'axios-mock-adapter'
import { isoDate } from '@/lib/dates'
import { newMock, renderReady, signIn, STUDENT } from '@/test/utils'

let mock: MockAdapter
const today = isoDate(new Date())
const dish = { id: 1, name: 'Dal Tadka', description: null, category: 'MAIN', veg: true, imageUrl: null, active: true }
const menu = (id: number, mealType: string, open: boolean) => ({ id, date: today, mealType, published: true, items: [dish], startTime: '12:30:00', endTime: '14:30:00', price: 60, bookingClosesAt: '2099-01-01T05:00:00Z', bookingOpen: open })
const booking = (o = {}) => ({ id: 9, dailyMenuId: 1, date: today, mealType: 'LUNCH', status: 'BOOKED', price: 60, qrToken: 'tok', startTime: '12:30:00', endTime: '14:30:00', bookingClosesAt: '2099-01-01T05:00:00Z', cancellable: true, dishes: ['Dal Tadka'], ...o })
const posts = (u: string) => mock.history.post.filter((r) => r.url === u)

beforeEach(() => { mock = newMock(); signIn(STUDENT) })
afterEach(() => mock.restore())

describe('menu & booking', () => {
  it('books an open meal and refreshes', async () => {
    mock.onGet(/\/menus\?/).reply(200, [menu(1, 'LUNCH', true)])
    mock.onGet(/\/bookings\/me/).reply(200, [])
    mock.onPost('/bookings').reply(201, booking())
    await renderReady('/app/menu')
    await userEvent.click(await screen.findByRole('button', { name: 'Book this meal' }))
    expect(JSON.parse(posts('/bookings')[0]!.data)).toEqual({ date: today, mealType: 'LUNCH' })
  })

  it('shows booked meals with Cancel, and closed meals without a button', async () => {
    mock.onGet(/\/menus\?/).reply(200, [menu(1, 'LUNCH', true), menu(2, 'DINNER', false)])
    mock.onGet(/\/bookings\/me/).reply(200, [booking()])
    mock.onDelete('/bookings/9').reply(200, booking({ status: 'CANCELLED' }))
    await renderReady('/app/menu')
    expect(await screen.findByText(/Booked ✓/)).toBeInTheDocument()
    expect(screen.getByText('Booking for this meal has closed.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(mock.history.delete).toHaveLength(1)
  })

  it('"Book all open meals" sends only open, unbooked meals in one bulk call', async () => {
    mock.onGet(/\/menus\?/).reply(200, [menu(1, 'LUNCH', true), menu(2, 'DINNER', true), menu(3, 'SNACKS', false)])
    mock.onGet(/\/bookings\/me/).reply(200, [booking()])
    mock.onPost('/bookings/bulk').reply(200, { booked: [booking({ id: 10, mealType: 'DINNER' })], skipped: [] })
    await renderReady('/app/menu')
    await userEvent.click(await screen.findByRole('button', { name: /Book all open meals \(1\)/ }))
    expect(JSON.parse(posts('/bookings/bulk')[0]!.data).items).toEqual([{ date: today, mealType: 'DINNER' }])
  })

  it('explains an empty day', async () => {
    mock.onGet(/\/menus\?/).reply(200, [])
    mock.onGet(/\/bookings\/me/).reply(200, [])
    await renderReady('/app/menu')
    expect(await screen.findByText('No menu for this day yet')).toBeInTheDocument()
  })
})

describe('my bookings', () => {
  it('lists upcoming meals and opens the QR pass', async () => {
    mock.onGet(/\/bookings\/me/).reply(200, [booking()])
    mock.onGet('/bookings/9/qr').reply(200, { bookingId: 9, qrToken: 'tok', payload: 'THALI:tok', status: 'BOOKED', date: today, mealType: 'LUNCH' })
    await renderReady('/app/bookings')
    await userEvent.click(await screen.findByRole('button', { name: /Pass/ }))
    expect(await screen.findByRole('img', { name: 'QR code for your meal pass' })).toBeInTheDocument()
    expect(screen.getByText(/It works once/)).toBeInTheDocument()
  })

  it('shows an empty state with a pointer to the menu', async () => {
    mock.onGet(/\/bookings\/me/).reply(200, [])
    await renderReady('/app/bookings')
    expect(await screen.findByText('Nothing booked yet')).toBeInTheDocument()
  })
})

describe('bills', () => {
  const bill = { id: 5, month: '2026-10', mealCount: 74, mealCharges: 3505, fixedCharges: 300, total: 3805, status: 'OVERDUE', dueDate: '2026-11-24' }

  it('pays with UPI: checkout, then confirm, then success', async () => {
    mock.onGet('/bills/me').reply(200, [bill])
    mock.onPost('/payments/checkout').reply(201, { id: 3, billId: 5, amount: 3805, method: 'UPI', txnRef: 'MOCK-1', status: 'PENDING', gatewayPayload: 'upi://pay?x=1' })
    mock.onPost('/payments/3/confirm').reply(200, { id: 3, status: 'SUCCESS', txnRef: 'MOCK-1', amount: 3805, method: 'UPI' })
    await renderReady('/app/bills')
    await userEvent.click(await screen.findByRole('button', { name: 'Pay now' }))
    await userEvent.click(screen.getByRole('button', { name: /Pay with UPI/ }))
    expect(await screen.findByRole('img', { name: 'UPI payment QR code' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: "I've paid" }))
    expect(await screen.findByText('Payment successful')).toBeInTheDocument()
    expect(JSON.parse(posts('/payments/checkout')[0]!.data)).toEqual({ billId: 5, method: 'UPI' })
  })

  it('paid bills have no Pay button; details show the breakdown', async () => {
    mock.onGet('/bills/me').reply(200, [{ ...bill, status: 'PAID' }])
    mock.onGet('/bills/5').reply(200, { ...bill, status: 'PAID', paidAt: null, lines: [{ date: '2026-10-05', mealType: 'LUNCH', status: 'CONSUMED', price: 60 }] })
    await renderReady('/app/bills')
    expect(await screen.findByText('paid')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Pay now' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Details' }))
    expect(await screen.findByText('Fixed monthly charge')).toBeInTheDocument()
    expect(within(screen.getByRole('list', { name: 'Meals in this bill' })).getAllByRole('listitem')).toHaveLength(1)
  })

  it('shows a payment error from the server', async () => {
    mock.onGet('/bills/me').reply(200, [bill])
    mock.onPost('/payments/checkout').reply(409, { code: 'BILL_ALREADY_PAID', detail: 'This bill has already been paid.' })
    await renderReady('/app/bills')
    await userEvent.click(await screen.findByRole('button', { name: 'Pay now' }))
    await userEvent.click(screen.getByRole('button', { name: /Pay with card/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('already been paid')
  })
})

describe('feedback', () => {
  it('needs a star rating before submitting, then sends it', async () => {
    mock.onGet('/feedback/me/pending').reply(200, [{ dailyMenuId: 4, date: today, mealType: 'DINNER', dishes: ['Kheer'] }])
    mock.onGet('/feedback/me').reply(200, [])
    mock.onPost('/feedback').reply(201, {})
    await renderReady('/app/feedback')
    const submit = await screen.findByRole('button', { name: 'Submit rating' })
    expect(submit).toBeDisabled()
    await userEvent.click(screen.getByRole('radio', { name: '4 stars' }))
    await userEvent.type(screen.getByLabelText(/Comment/), 'Tasty')
    await userEvent.click(submit)
    expect(JSON.parse(posts('/feedback')[0]!.data)).toEqual({ dailyMenuId: 4, rating: 4, comment: 'Tasty' })
  })

  it('shows past ratings and an all-caught-up state', async () => {
    mock.onGet('/feedback/me/pending').reply(200, [])
    mock.onGet('/feedback/me').reply(200, [{ id: 1, dailyMenuId: 4, date: today, mealType: 'LUNCH', rating: 5, comment: 'Great', createdAt: '' }])
    await renderReady('/app/feedback')
    expect(await screen.findByText('All caught up')).toBeInTheDocument()
    expect(screen.getByText('“Great”')).toBeInTheDocument()
  })
})

describe('complaints', () => {
  it('raises a complaint with a category, title and details', async () => {
    mock.onGet('/complaints/me').reply(200, [])
    mock.onPost('/complaints').reply(201, { id: 1 })
    await renderReady('/app/complaints')
    await userEvent.click(await screen.findByRole('button', { name: /New complaint/ }))
    const submit = screen.getByRole('button', { name: 'Submit' })
    expect(submit).toBeDisabled()
    await userEvent.selectOptions(screen.getByLabelText('Category'), 'HYGIENE')
    await userEvent.type(screen.getByLabelText('Title'), 'Dirty plates')
    await userEvent.type(screen.getByLabelText('Details'), 'Residue on plates')
    await userEvent.click(submit)
    expect(JSON.parse(posts('/complaints')[0]!.data)).toMatchObject({ category: 'HYGIENE', title: 'Dirty plates', description: 'Residue on plates' })
  })

  it('opens the timeline and hides the comment box once closed', async () => {
    const c = { id: 2, category: 'SERVICE', title: 'Long queue', description: 'Slow', priority: 'MEDIUM', status: 'RESOLVED', assignedToName: 'Ramesh', createdAt: '', updatedAt: '',
      updates: [{ id: 1, authorName: 'Ramesh', authorRole: 'STAFF', message: 'Opened a second counter', newStatus: 'RESOLVED', createdAt: '' }] }
    mock.onGet('/complaints/me').reply(200, [c])
    mock.onGet('/complaints/2').reply(200, c)
    await renderReady('/app/complaints')
    await userEvent.click(await screen.findByText('Long queue'))
    expect(await screen.findByText(/Opened a second counter/)).toBeInTheDocument()
    expect(screen.getByText('This complaint is closed.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Add a comment')).not.toBeInTheDocument()
  })
})

describe('profile', () => {
  it('saves details and validates the new password', async () => {
    mock.onGet('/me/profile').reply(200, STUDENT)
    mock.onPut('/me/profile').reply(200, { ...STUDENT, roomNo: '400' })
    await renderReady('/app/profile')
    const room = await screen.findByLabelText('Room number')
    await userEvent.clear(room)
    await userEvent.type(room, '400')
    await userEvent.click(screen.getByRole('button', { name: 'Save changes' }))
    expect(JSON.parse(mock.history.put[0]!.data).roomNo).toBe('400')
    await userEvent.type(screen.getByLabelText('New password'), 'weak')
    expect(await screen.findByText('At least 8 characters')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Update password' })).toBeDisabled()
  })
})
