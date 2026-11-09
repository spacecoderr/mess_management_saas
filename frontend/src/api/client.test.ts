import MockAdapter from 'axios-mock-adapter'
import { ApiError } from '@/lib/errors'
import { useAuthStore } from '@/store/authStore'
import { authFor, STUDENT } from '@/test/utils'
import { get, http, post } from './client'

let mock: MockAdapter
const refreshCalls = () => mock.history.post.filter((r) => r.url === '/auth/refresh').length

beforeEach(() => {
  mock = new MockAdapter(http)
  useAuthStore.getState().setSession(authFor(STUDENT, 'old'))
})
afterEach(() => mock.restore())

describe('http client', () => {
  it('sends the access token as a Bearer header', async () => {
    mock.onGet('/ping').reply(200, { ok: true })
    await get('/ping')
    expect(mock.history.get[0]!.headers!.Authorization).toBe('Bearer access-old')
  })

  it('does not send a token on public (skipAuth) calls', async () => {
    mock.onPost('/auth/login').reply(200, authFor(STUDENT))
    await post('/auth/login', {}, { skipAuth: true })
    expect(mock.history.post[0]!.headers!.Authorization).toBeUndefined()
  })

  it('on 401 refreshes once, stores the new tokens and retries the original request', async () => {
    mock.onGet('/ping').replyOnce(401).onGet('/ping').reply(200, { ok: true })
    mock.onPost('/auth/refresh').reply(200, authFor(STUDENT, 'new'))

    await expect(get('/ping')).resolves.toEqual({ ok: true })

    expect(refreshCalls()).toBe(1)
    expect(JSON.parse(mock.history.post[0]!.data)).toEqual({ refreshToken: 'refresh-old' })
    expect(mock.history.get[1]!.headers!.Authorization).toBe('Bearer access-new')   // the retry uses the new token
    expect(useAuthStore.getState().refreshToken).toBe('refresh-new')
  })

  it('shares ONE refresh between requests that fail together', async () => {
    for (const url of ['/a', '/b', '/c']) mock.onGet(url).replyOnce(401).onGet(url).reply(200, url)
    mock.onPost('/auth/refresh').reply(200, authFor(STUDENT, 'new'))

    const results = await Promise.all([get('/a'), get('/b'), get('/c')])

    expect(results).toEqual(['/a', '/b', '/c'])
    expect(refreshCalls()).toBe(1)   // not three: refresh tokens are single-use, a second call would fail
  })

  it('signs the user out (flagged as expired) when the refresh token is rejected', async () => {
    mock.onGet('/ping').reply(401)
    mock.onPost('/auth/refresh').reply(401, { code: 'REFRESH_TOKEN_INVALID', detail: 'Your session has expired.' })

    await expect(get('/ping')).rejects.toMatchObject({ code: 'REFRESH_TOKEN_INVALID' })

    const s = useAuthStore.getState()
    expect(s.user).toBeNull()
    expect(s.sessionExpired).toBe(true)
  })

  it('does not loop: a retried request that is still 401 fails instead of refreshing again', async () => {
    mock.onGet('/ping').reply(401)
    mock.onPost('/auth/refresh').reply(200, authFor(STUDENT, 'new'))
    await expect(get('/ping')).rejects.toBeInstanceOf(ApiError)
    expect(refreshCalls()).toBe(1)
  })

  it('never tries to refresh for public calls (a wrong password is not an expired session)', async () => {
    mock.onPost('/auth/login').reply(401, { code: 'INVALID_CREDENTIALS', detail: 'Incorrect email or password.' })
    await expect(post('/auth/login', {}, { skipAuth: true })).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS', status: 401 })
    expect(refreshCalls()).toBe(0)
    expect(useAuthStore.getState().user).not.toBeNull()   // still signed in
  })

  it('does not refresh when there is no refresh token', async () => {
    useAuthStore.getState().clear()
    mock.onGet('/ping').reply(401, { code: 'UNAUTHENTICATED', detail: 'Please log in to continue.' })
    await expect(get('/ping')).rejects.toMatchObject({ code: 'UNAUTHENTICATED' })
    expect(refreshCalls()).toBe(0)
  })

  it('turns server errors into ApiError with the problem+json code and field errors', async () => {
    mock.onPost('/x').reply(400, { code: 'VALIDATION_FAILED', detail: 'Validation failed', errors: [{ field: 'email', message: 'bad email' }] })
    const err = (await post('/x', {}).catch((e) => e)) as ApiError
    expect(err).toBeInstanceOf(ApiError)
    expect(err.fieldErrors).toEqual({ email: 'bad email' })
  })

  it('reports an unreachable server as a network ApiError', async () => {
    mock.onGet('/ping').networkError()
    const err = await get('/ping').catch((e) => e)
    expect(err).toMatchObject({ isNetwork: true, code: 'NETWORK' })
  })
})
