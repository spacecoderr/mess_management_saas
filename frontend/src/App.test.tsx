import { render, screen } from '@testing-library/react'
import MockAdapter from 'axios-mock-adapter'
import { http } from '@/api/client'
import App from './App'

let mock: MockAdapter
beforeEach(() => {
  mock = new MockAdapter(http)
  mock.onGet('/health').reply(200, { status: 'UP', service: 'thali', database: 'UP', timestamp: '' })
})
afterEach(() => mock.restore())

describe('App (real BrowserRouter + providers)', () => {
  it('serves the landing page at /', async () => {
    window.history.pushState({}, '', '/')
    render(<App />)
    expect(screen.getByRole('heading', { level: 1 })).toBeInTheDocument()
    await screen.findByText('API & database online')
  })

  it('shows a not-found page for unknown routes', async () => {
    window.history.pushState({}, '', '/definitely-not-a-page')
    render(<App />)
    expect(await screen.findByText('Page not found')).toBeInTheDocument()
  })

  it('opens the login page at /login', async () => {
    window.history.pushState({}, '', '/login')
    render(<App />)
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it('applies the saved theme to <html>', async () => {
    localStorage.setItem('thali-theme', JSON.stringify({ state: { mode: 'dark' }, version: 0 }))
    const { useThemeStore } = await import('@/store/themeStore')
    await useThemeStore.persist.rehydrate()
    window.history.pushState({}, '', '/definitely-not-a-page')
    render(<App />)
    await screen.findByText('Page not found')
    expect(document.documentElement.dataset.theme).toBe('dark')
  })
})
