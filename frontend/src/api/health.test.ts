import MockAdapter from 'axios-mock-adapter'
import { http } from './client'
import { getHealth } from './health'

const ok = { status: 'UP', service: 'thali', database: 'UP', timestamp: '2026-11-09T00:00:00Z' }
let mock: MockAdapter
beforeEach(() => (mock = new MockAdapter(http)))
afterEach(() => mock.restore())

describe('getHealth', () => {
  it('calls the public health endpoint (no auth header) and returns the body', async () => {
    mock.onGet('/health').reply(200, ok)
    await expect(getHealth()).resolves.toEqual(ok)
    expect(mock.history.get[0]!.headers!.Authorization).toBeUndefined()
  })

  it('rejects with the HTTP status when the API is unhealthy', async () => {
    mock.onGet('/health').reply(503, { code: 'HTTP_503', detail: 'down' })
    await expect(getHealth()).rejects.toMatchObject({ status: 503 })
  })

  it('rejects on network failure', async () => {
    mock.onGet('/health').networkError()
    await expect(getHealth()).rejects.toMatchObject({ isNetwork: true })
  })
})
