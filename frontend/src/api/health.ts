import { get } from './client'

export interface HealthResponse {
  status: 'UP' | 'DEGRADED'
  service: string
  database: 'UP' | 'DOWN'
  timestamp: string
}

export const getHealth = () => get<HealthResponse>('/health', { skipAuth: true })
