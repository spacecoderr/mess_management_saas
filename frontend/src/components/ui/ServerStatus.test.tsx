import { render, screen } from '@testing-library/react'
import MockAdapter from 'axios-mock-adapter'
import { http } from '@/api/client'
import { ServerStatus } from './ServerStatus'

let mock: MockAdapter
beforeEach(() => (mock = new MockAdapter(http)))
afterEach(() => mock.restore())

describe('ServerStatus', () => {
  it('shows a waking-up message while the first request is in flight', () => {
    mock.onGet('/health').reply(() => new Promise(() => {}))   // never resolves
    render(<ServerStatus />)
    expect(screen.getByText('Waking server…')).toBeInTheDocument()
  })

  it('shows online when the API and database are up', async () => {
    mock.onGet('/health').reply(200, { status: 'UP', service: 'thali', database: 'UP', timestamp: '' })
    render(<ServerStatus />)
    expect(await screen.findByText('API & database online')).toBeInTheDocument()
  })

  it('shows offline when the API reports a degraded database', async () => {
    mock.onGet('/health').reply(200, { status: 'DEGRADED', service: 'thali', database: 'DOWN', timestamp: '' })
    render(<ServerStatus />)
    expect(await screen.findByText('API offline')).toBeInTheDocument()
  })

  it('shows offline when the request fails', async () => {
    mock.onGet('/health').networkError()
    render(<ServerStatus />)
    expect(await screen.findByText('API offline')).toBeInTheDocument()
  })
})
