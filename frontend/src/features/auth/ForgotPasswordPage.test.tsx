import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type MockAdapter from 'axios-mock-adapter'
import { newMock, renderReady } from '@/test/utils'

let mock: MockAdapter
beforeEach(() => (mock = newMock()))
afterEach(() => mock.restore())

async function reachResetStep() {
  mock.onPost('/auth/otp/request').reply(200, { message: 'sent' })
  await renderReady('/forgot-password')
  await userEvent.type(screen.getByLabelText('Email'), 'z900002@iiit-bh.ac.in')
  await userEvent.click(screen.getByRole('button', { name: 'Send code' }))
  await screen.findByText(/a 6-digit code is on its way/)
}

describe('forgot password', () => {
  it('requests a RESET_PASSWORD code for the entered email', async () => {
    await reachResetStep()
    expect(JSON.parse(mock.history.post[0]!.data)).toEqual({ email: 'z900002@iiit-bh.ac.in', purpose: 'RESET_PASSWORD' })
  })

  it('resets the password with code + new password and sends the user to log in', async () => {
    mock.onPost('/auth/password/reset').reply(200, { message: 'ok' })
    await reachResetStep()
    await userEvent.click(screen.getByLabelText('Digit 1 of 6'))
    await userEvent.keyboard('246810')
    await userEvent.type(screen.getByLabelText('New password'), 'Brand1New')
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'Brand1New')
    await userEvent.click(screen.getByRole('button', { name: 'Update password' }))

    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(JSON.parse(mock.history.post.find((r) => r.url === '/auth/password/reset')!.data)).toEqual({ email: 'z900002@iiit-bh.ac.in', code: '246810', newPassword: 'Brand1New' })
  })

  it('needs the full code, a valid password and a matching confirmation', async () => {
    await reachResetStep()
    await userEvent.type(screen.getByLabelText('New password'), 'weak')
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'nope')
    await userEvent.click(screen.getByRole('button', { name: 'Update password' }))
    expect(await screen.findByText('Enter the 6-digit code')).toBeInTheDocument()
    expect(screen.getByText('At least 8 characters')).toBeInTheDocument()
    expect(screen.getByText("Passwords don't match")).toBeInTheDocument()
    expect(mock.history.post.filter((r) => r.url === '/auth/password/reset')).toHaveLength(0)
  })

  it('shows the server error for a wrong code', async () => {
    mock.onPost('/auth/password/reset').reply(400, { code: 'OTP_INVALID', detail: 'That code is invalid or has expired.' })
    await reachResetStep()
    await userEvent.click(screen.getByLabelText('Digit 1 of 6'))
    await userEvent.keyboard('000000')
    await userEvent.type(screen.getByLabelText('New password'), 'Brand1New')
    await userEvent.type(screen.getByLabelText('Confirm new password'), 'Brand1New')
    await userEvent.click(screen.getByRole('button', { name: 'Update password' }))
    expect(await screen.findByText('That code is invalid or has expired.')).toBeInTheDocument()
  })
})
