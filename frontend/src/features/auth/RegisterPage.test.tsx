import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type MockAdapter from 'axios-mock-adapter'
import { newMock, renderReady } from '@/test/utils'

let mock: MockAdapter
beforeEach(() => (mock = newMock()))
afterEach(() => mock.restore())

const EMAIL = 'b424777@iiit-bh.ac.in'
const calls = (url: string) => mock.history.post.filter((r) => r.url === url)

/** Walks steps 1 and 2: email -> code. Leaves the form on the "Your details" step. */
async function reachDetails() {
  mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
  mock.onPost('/auth/otp/verify').reply(200, { registrationToken: 'reg-token', auth: null })
  await renderReady('/register')
  await userEvent.type(screen.getByLabelText('College email'), EMAIL)
  await userEvent.click(screen.getByRole('button', { name: 'Send verification code' }))
  await userEvent.click(await screen.findByLabelText('Digit 1 of 6'))
  await userEvent.keyboard('574564')
  await screen.findByText(`${EMAIL} verified`, { exact: false })
}

async function fillDetails(over: Partial<Record<string, string>> = {}) {
  const v = { name: 'Priya Test Student', phone: '9876500777', block: 'Girls Hostel A', room: '212', password: 'Test12345', confirm: 'Test12345', ...over }
  await userEvent.type(screen.getByLabelText('Full name'), v.name)
  await userEvent.type(screen.getByLabelText('Phone'), v.phone)
  await userEvent.type(screen.getByLabelText('Hostel block'), v.block)
  await userEvent.type(screen.getByLabelText('Room number'), v.room)
  await userEvent.click(screen.getByLabelText('Non-veg'))
  await userEvent.type(screen.getByLabelText('Create a password'), v.password)
  await userEvent.type(screen.getByLabelText('Confirm password'), v.confirm)
}
const create = () => userEvent.click(screen.getByRole('button', { name: 'Create account' }))

describe('step 1: college email', () => {
  it('rejects non-college emails without calling the API', async () => {
    await renderReady('/register')
    await userEvent.type(screen.getByLabelText('College email'), 'someone@gmail.com')
    await userEvent.click(screen.getByRole('button', { name: 'Send verification code' }))
    expect(await screen.findByText(/Use your IIIT Bhubaneswar email/)).toBeInTheDocument()
    expect(calls('/auth/otp/request')).toHaveLength(0)
  })

  it('shows the roll number and batch while typing', async () => {
    await renderReady('/register')
    await userEvent.type(screen.getByLabelText('College email'), EMAIL)
    expect(screen.getByText('Roll number B424777 · batch of 2024')).toBeInTheDocument()
  })

  it('offers to log in when the email is already registered', async () => {
    mock.onPost('/auth/otp/request').reply(409, { code: 'EMAIL_ALREADY_REGISTERED', detail: 'This email is already registered. Try logging in.' })
    await renderReady('/register')
    await userEvent.type(screen.getByLabelText('College email'), EMAIL)
    await userEvent.click(screen.getByRole('button', { name: 'Send verification code' }))
    expect(await screen.findByText(/already registered/)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Log in instead' })).toHaveAttribute('href', '/login')
  })
})

describe('step 2: verify', () => {
  it('sends the code for REGISTER and moves on when it is right', async () => {
    await reachDetails()
    expect(JSON.parse(calls('/auth/otp/verify')[0]!.data)).toEqual({ email: EMAIL, purpose: 'REGISTER', code: '574564' })
    expect(JSON.parse(calls('/auth/otp/request')[0]!.data)).toEqual({ email: EMAIL, purpose: 'REGISTER' })
  })

  it('a wrong code keeps the user on this step with the error', async () => {
    mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
    mock.onPost('/auth/otp/verify').reply(400, { code: 'OTP_INVALID', detail: 'That code is invalid or has expired.' })
    await renderReady('/register')
    await userEvent.type(screen.getByLabelText('College email'), EMAIL)
    await userEvent.click(screen.getByRole('button', { name: 'Send verification code' }))
    await userEvent.click(await screen.findByLabelText('Digit 1 of 6'))
    await userEvent.keyboard('111111')
    expect(await screen.findByText('That code is invalid or has expired.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Full name')).not.toBeInTheDocument()
  })
})

describe('step 3: your details', () => {
  it('creates the account with the registration token and lands on the pending page', async () => {
    mock.onPost('/auth/register').reply(201, { id: 32, rollNo: 'B424777', approvalStatus: 'PENDING' })
    await reachDetails()
    await fillDetails()
    await create()

    expect(await screen.findByRole('heading', { name: "You're almost in" })).toBeInTheDocument()
    expect(screen.getByText(EMAIL)).toBeInTheDocument()
    expect(JSON.parse(calls('/auth/register')[0]!.data)).toEqual({
      registrationToken: 'reg-token', name: 'Priya Test Student', phone: '9876500777', hostelBlock: 'Girls Hostel A', roomNo: '212', dietPref: 'NON_VEG', password: 'Test12345',
    })
  })

  it('blocks a mismatched password confirmation and a weak password', async () => {
    await reachDetails()
    await fillDetails({ password: 'weak', confirm: 'different' })
    await create()
    expect(await screen.findByText('At least 8 characters')).toBeInTheDocument()
    expect(screen.getByText("Passwords don't match")).toBeInTheDocument()
    expect(calls('/auth/register')).toHaveLength(0)
  })

  it('maps field errors from the server onto the right inputs', async () => {
    mock.onPost('/auth/register').reply(400, { code: 'VALIDATION_FAILED', detail: 'Validation failed', errors: [{ field: 'phone', message: 'Enter a valid phone number' }] })
    await reachDetails()
    await fillDetails()
    await create()
    expect(await screen.findByLabelText('Phone')).toHaveAccessibleDescription('Enter a valid phone number')
  })

  it('goes back to step 1 with a notice if the email verification expired meanwhile', async () => {
    mock.onPost('/auth/register').reply(400, { code: 'REGISTRATION_TOKEN_INVALID', detail: 'Your email verification has expired.' })
    await reachDetails()
    await fillDetails()
    await create()
    expect(await screen.findByText('Your email verification expired. Please verify again.')).toBeInTheDocument()
    expect(screen.getByLabelText('College email')).toBeInTheDocument()
  })

  it('food preference defaults to vegetarian', async () => {
    await reachDetails()
    expect(screen.getByLabelText('Vegetarian')).toBeChecked()
  })
})
