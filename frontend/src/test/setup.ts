import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach, beforeEach, vi } from 'vitest'
import { useAuthStore } from '@/store/authStore'
import { useThemeStore } from '@/store/themeStore'

beforeEach(() => {
  localStorage.clear()
  // zustand stores live in module memory, so reset them: no test may inherit another test's login or theme
  useAuthStore.setState({ accessToken: null, refreshToken: null, user: null, sessionExpired: false })
  useThemeStore.setState({ mode: 'system' })
  document.documentElement.dataset.theme = 'light'
})

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

// jsdom has no IntersectionObserver; framer-motion's whileInView needs one.
class FakeIntersectionObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() { return [] }
}
globalThis.IntersectionObserver = FakeIntersectionObserver as unknown as typeof IntersectionObserver

// "Reduce motion" is on in tests, so animated counters show their final value immediately (no waiting on tweens).
window.matchMedia = ((query: string) => ({
  matches: query.includes('prefers-reduced-motion'),
  media: query,
  onchange: null,
  addEventListener: () => {},
  removeEventListener: () => {},
  addListener: () => {},
  removeListener: () => {},
  dispatchEvent: () => false,
})) as unknown as typeof window.matchMedia

configure({ asyncUtilTimeout: 5000 })   // lazy chunks (charts, pages) can be slow to load on a busy machine
