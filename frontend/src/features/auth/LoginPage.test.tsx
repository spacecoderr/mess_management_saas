import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type MockAdapter from 'axios-mock-adapter'
import { useAuthStore } from '@/store/authStore'
import { authFor, mockHomeData, newMock, renderReady, STAFF, STUDENT } from '@/test/utils'

let mock: MockAdapter
beforeEach(() => {
  mock = newMock()
  mockHomeData(mock)
})
afterEach(() => mock.restore())

const type = async (label: string, text: string) => userEvent.type(screen.getByLabelText(label), text)
const submit = () => userEvent.click(screen.getByRole('button', { name: 'Log in' }))
const loginCalls = () => mock.history.post.filter((r) => r.url === '/auth/login')

describe('password login', () => {
  it('logs a student in and lands on their home page', async () => {
    mock.onPost('/auth/login').reply(200, authFor(STUDENT))
    await renderReady('/login')
    await type('Email', 'z900002@iiit-bh.ac.in')
    await type('Password', 'Demo@1234')
    await submit()

    expect(await screen.findByRole('heading', { name: /Ananya/ })).toBeInTheDocument()
    expect(JSON.parse(loginCalls()[0]!.data)).toEqual({ email: 'z900002@iiit-bh.ac.in', password: 'Demo@1234' })
    expect(useAuthStore.getState().user?.role).toBe('STUDENT')
  })

  it('validates before calling the API', async () => {
    await renderReady('/login')
    await submit()
    expect(await screen.findByText('Enter your email')).toBeInTheDocument()
    expect(screen.getByText('Enter your password')).toBeInTheDocument()
    await type('Email', 'not-an-email')
    await submit()
    expect(await screen.findByText('Enter a valid email address')).toBeInTheDocument()
    expect(loginCalls()).toHaveLength(0)
  })

  it('shows the server message for a wrong password and stays signed out', async () => {
    mock.onPost('/auth/login').reply(401, { code: 'INVALID_CREDENTIALS', detail: 'Incorrect email or password.' })
    await renderReady('/login')
    await type('Email', 'z900002@iiit-bh.ac.in')
    await type('Password', 'wrong')
    await submit()
    expect(await screen.findByRole('alert')).toHaveTextContent('Incorrect email or password.')
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('tells a pending student what is happening, with a link to learn more', async () => {
    mock.onPost('/auth/login').reply(403, { code: 'ACCOUNT_PENDING_APPROVAL', detail: 'Your account is waiting for admin approval.' })
    await renderReady('/login')
    await type('Email', 'b424777@iiit-bh.ac.in')
    await type('Password', 'Test12345')
    await submit()
    expect(await screen.findByText(/waiting for admin approval/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'What happens next?' })).toHaveAttribute('href', '/pending-approval')
  })

  it('explains an unreachable server instead of failing silently', async () => {
    mock.onPost('/auth/login').networkError()
    await renderReady('/login')
    await type('Email', 'z900002@iiit-bh.ac.in')
    await type('Password', 'Demo@1234')
    await submit()
    expect(await screen.findByRole('alert')).toHaveTextContent(/can't reach the server/i)
  })

  it('returns a user to the page they were trying to open', async () => {
    mock.onPost('/auth/login').reply(200, authFor(STAFF))
    await renderReady('/app/menu-today')   // signed out -> bounced to /login with the destination remembered
    await screen.findByRole('heading', { name: 'Welcome back' })
    await type('Email', 'staff1@thali.local')
    await type('Password', 'Demo@1234')
    await submit()
    expect(await screen.findByRole('heading', { name: "Today's menu" })).toBeInTheDocument()
  })

  it('ignores a remembered page the role may not open and goes home instead', async () => {
    mock.onPost('/auth/login').reply(200, authFor(STUDENT))
    await renderReady('/app/dashboard')   // an admin page; a student logs in
    await screen.findByRole('heading', { name: 'Welcome back' })
    await type('Email', 'z900002@iiit-bh.ac.in')
    await type('Password', 'Demo@1234')
    await submit()
    expect(await screen.findByRole('heading', { name: /Ananya/ })).toBeInTheDocument()
  })

  it('shows a notice when the previous session expired', async () => {
    useAuthStore.getState().clear(true)
    await renderReady('/login')
    expect(await screen.findByText(/session expired/i)).toBeInTheDocument()
  })
})

describe('demo accounts', () => {
  it('one click fills the form for each role', async () => {
    await renderReady('/login')
    await userEvent.click(screen.getByRole('button', { name: 'Admin' }))
    expect(screen.getByLabelText('Email')).toHaveValue('admin@thali.local')
    expect(screen.getByLabelText('Password')).toHaveValue('Admin@12345')
    await userEvent.click(screen.getByRole('button', { name: 'Student' }))
    expect(screen.getByLabelText('Email')).toHaveValue('z900002@iiit-bh.ac.in')
  })
})

describe('email code login', () => {
  const email = 'b423046@iiit-bh.ac.in'

  it('only accepts a college email on this tab', async () => {
    await renderReady('/login')
    await userEvent.click(screen.getByRole('tab', { name: 'Email code' }))
    await type('College email', 'someone@gmail.com')
    await userEvent.click(screen.getByRole('button', { name: 'Email me a code' }))
    expect(await screen.findByText(/Use your IIIT Bhubaneswar email/)).toBeInTheDocument()
    expect(mock.history.post.filter((r) => r.url === '/auth/otp/request')).toHaveLength(0)
  })

  it('requests a code, then logs in as soon as the 6th digit is typed', async () => {
    mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
    mock.onPost('/auth/otp/verify').reply(200, { registrationToken: null, auth: authFor(STUDENT) })
    await renderReady('/login')
    await userEvent.click(screen.getByRole('tab', { name: 'Email code' }))
    await type('College email', email)
    await userEvent.click(screen.getByRole('button', { name: 'Email me a code' }))

    expect(await screen.findByText(email)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Resend code in \d+s/ })).toBeDisabled()
    await userEvent.click(screen.getByLabelText('Digit 1 of 6'))
    await userEvent.keyboard('123456')

    expect(await screen.findByRole('heading', { name: /Ananya/ })).toBeInTheDocument()
    expect(JSON.parse(mock.history.post.find((r) => r.url === '/auth/otp/verify')!.data)).toEqual({ email, purpose: 'LOGIN', code: '123456' })
  })

  it('a wrong code shows the error and clears the boxes for another try', async () => {
    mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
    mock.onPost('/auth/otp/verify').reply(400, { code: 'OTP_INVALID', detail: 'That code is invalid or has expired.' })
    await renderReady('/login')
    await userEvent.click(screen.getByRole('tab', { name: 'Email code' }))
    await type('College email', email)
    await userEvent.click(screen.getByRole('button', { name: 'Email me a code' }))
    await userEvent.click(await screen.findByLabelText('Digit 1 of 6'))
    await userEvent.keyboard('000000')

    expect(await screen.findByText('That code is invalid or has expired.')).toBeInTheDocument()
    expect((screen.getByLabelText('Digit 1 of 6') as HTMLInputElement).value).toBe('')
    expect(useAuthStore.getState().user).toBeNull()
  })

  it('"Use a different email" goes back to the email step', async () => {
    mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
    await renderReady('/login')
    await userEvent.click(screen.getByRole('tab', { name: 'Email code' }))
    await type('College email', email)
    await userEvent.click(screen.getByRole('button', { name: 'Email me a code' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Use a different email' }))
    expect(await screen.findByLabelText('College email')).toBeInTheDocument()
  })
})
