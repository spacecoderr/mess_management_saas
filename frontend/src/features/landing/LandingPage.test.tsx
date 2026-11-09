import { render, screen, within } from '@testing-library/react'
import MockAdapter from 'axios-mock-adapter'
import { MemoryRouter } from 'react-router-dom'
import { http } from '@/api/client'
import { signIn, STAFF, STUDENT } from '@/test/utils'
import LandingPage from './LandingPage'

let mock: MockAdapter
beforeEach(() => {
  mock = new MockAdapter(http)
  mock.onGet('/health').reply(200, { status: 'UP', service: 'thali', database: 'UP', timestamp: '' })
})
afterEach(() => mock.restore())

/** The page pings the API on mount; waiting for that result keeps state updates inside the test (no act() warnings). */
const renderSettled = async () => {
  render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><LandingPage /></MemoryRouter>)
  await screen.findByText('API & database online')
}

describe('LandingPage', () => {
  it('renders the hero headline and call-to-action', async () => {
    await renderSettled()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent('Your mess, made effortless.')
    expect(screen.getByRole('link', { name: /get started/i })).toHaveAttribute('href', '/register')
  })

  it('links log in and register to the real pages', async () => {
    await renderSettled()
    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: 'Log in' })).toHaveAttribute('href', '/login')
    expect(within(nav).getByRole('link', { name: 'Register' })).toHaveAttribute('href', '/register')
  })

  it('lists all four meals in the sample menu', async () => {
    await renderSettled()
    for (const meal of ['Breakfast', 'Lunch', 'Snacks', 'Dinner']) expect(screen.getByText(meal)).toBeInTheDocument()
  })

  it('describes every module of the system', async () => {
    await renderSettled()
    for (const title of ['Daily meal booking', 'QR attendance', 'Menu management', 'Ratings & feedback', 'Complaints', 'Monthly billing', 'Analytics', 'College-verified']) {
      expect(screen.getByRole('heading', { name: title })).toBeInTheDocument()
    }
  })

  it('mentions the college email requirement', async () => {
    await renderSettled()
    expect(screen.getByText(/@iiit-bh\.ac\.in/)).toBeInTheDocument()
  })

  it('has a theme toggle', async () => {
    await renderSettled()
    expect(screen.getByRole('button', { name: /Switch to (dark|light) theme/ })).toBeInTheDocument()
  })

  it('signed-in visitors get "Open app" pointing at their own home instead of log in / register', async () => {
    signIn(STAFF)
    await renderSettled()
    const nav = screen.getByRole('navigation')
    expect(within(nav).getByRole('link', { name: 'Open app' })).toHaveAttribute('href', '/app/serve')
    expect(within(nav).queryByRole('link', { name: 'Log in' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Open my dashboard/ })).toHaveAttribute('href', '/app/serve')
  })

  it('a student is pointed at the student home', async () => {
    signIn(STUDENT)
    await renderSettled()
    expect(screen.getByRole('link', { name: 'Open app' })).toHaveAttribute('href', '/app/home')
  })
})
