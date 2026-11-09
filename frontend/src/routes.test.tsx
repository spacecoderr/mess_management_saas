import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ADMIN, mockHomeData, newMock, renderRoute, signIn, STAFF, STUDENT } from '@/test/utils'
import { NAV } from '@/components/layout/nav'
import { useAuthStore } from '@/store/authStore'
import type MockAdapter from 'axios-mock-adapter'

let mock: MockAdapter
beforeEach(() => {
  mock = newMock()
  mockHomeData(mock)
  mock.onPost('/auth/logout').reply(200, { message: 'Logged out.' })
})
afterEach(() => mock.restore())

describe('route guards', () => {
  it('sends signed-out visitors to /login', async () => {
    renderRoute('/app/home')
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
  })

  it.each([
    ['STUDENT', STUDENT, 'Today\'s meals'],
    ['STAFF', STAFF, 'Serve counter'],
    ['ADMIN', ADMIN, 'Dashboard'],
  ] as const)('/app sends a %s to their own home', async (_role, user, heading) => {
    signIn(user)
    renderRoute('/app')
    expect(await screen.findByRole('heading', { name: heading })).toBeInTheDocument()
  })

  it.each([
    ['a student', STUDENT, ['/app/dashboard', '/app/serve', '/app/students', '/app/billing']],
    ['staff', STAFF, ['/app/home', '/app/dashboard', '/app/bills', '/app/students']],
    ['an admin', ADMIN, ['/app/home', '/app/serve', '/app/bookings', '/app/menu-today']],
  ] as const)('%s is shown the 403 page for other roles\' pages', async (_who, user, paths) => {
    for (const path of paths) {
      signIn(user)
      const { unmount } = renderRoute(path)
      expect(await screen.findByText('Not allowed')).toBeInTheDocument()
      unmount()
    }
  })

  it('redirects a signed-in user away from /login and /register to their home', async () => {
    signIn(STUDENT)
    const { unmount } = renderRoute('/login')
    expect(await screen.findByRole('heading', { name: "Today's meals" })).toBeInTheDocument()
    unmount()
    renderRoute('/register')
    expect(await screen.findByRole('heading', { name: "Today's meals" })).toBeInTheDocument()
  })

  it('shows a friendly 404 for unknown pages, with a way back', async () => {
    renderRoute('/nope/not/here')
    expect(await screen.findByText('Page not found')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Back to the homepage' })).toHaveAttribute('href', '/')
  })

  it('the 403 page links a signed-in user back to their own home', async () => {
    signIn(STAFF)
    renderRoute('/app/dashboard')
    expect(await screen.findByRole('link', { name: 'Back to my home' })).toHaveAttribute('href', '/app/serve')
  })
})

describe('app shell navigation', () => {
  it.each([
    ['STUDENT', STUDENT],
    ['STAFF', STAFF],
    ['ADMIN', ADMIN],
  ] as const)('%s sees exactly their own menu items', async (role, user) => {
    signIn(user)
    renderRoute('/app')
    await screen.findByRole('navigation', { name: 'Main' })
    const links = screen.getAllByRole('link').filter((a) => a.getAttribute('href')?.startsWith('/app/'))
    const hrefs = [...new Set(links.map((a) => a.getAttribute('href')))]
    expect(hrefs.sort()).toEqual(NAV[role].map((n) => n.to).sort())
  })

  it('logging out revokes the refresh token, clears the session and lands on /login', async () => {
    signIn(STUDENT)
    renderRoute('/app/home')
    const logout = (await screen.findAllByRole('button', { name: 'Log out' }))[0]!
    await userEvent.click(logout)

    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(useAuthStore.getState().user).toBeNull()
    expect(useAuthStore.getState().refreshToken).toBeNull()
    expect(JSON.parse(mock.history.post.find((r) => r.url === '/auth/logout')!.data)).toEqual({ refreshToken: 'refresh-a' })
  })

  it('still logs out locally if the server cannot be reached', async () => {
    mock.onPost('/auth/logout').networkError()
    signIn(STUDENT)
    renderRoute('/app/home')
    await userEvent.click((await screen.findAllByRole('button', { name: 'Log out' }))[0]!)
    expect(await screen.findByRole('heading', { name: 'Welcome back' })).toBeInTheDocument()
    expect(useAuthStore.getState().user).toBeNull()
  })
})
