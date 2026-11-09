import axios from 'axios'
import type { ProblemDetail } from '@/types/api'

/** Every failed API call is turned into one of these, so UI code never touches raw axios errors. */
export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string,
    readonly fieldErrors: Record<string, string> = {},
    readonly isNetwork = false,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error
  // isAxiosError() checks a flag, not class identity, so it also works for errors created by another copy of axios
  if (axios.isAxiosError(error)) {
    if (!error.response) {
      const timeout = error.code === 'ECONNABORTED'
      return new ApiError(
        timeout ? 'The server is taking too long to respond. It may be waking up, please try again.' : "Can't reach the server. Check your connection and try again.",
        0,
        timeout ? 'TIMEOUT' : 'NETWORK',
        {},
        true,
      )
    }
    const body = (error.response.data ?? {}) as Partial<ProblemDetail>
    const fields: Record<string, string> = {}
    body.errors?.forEach((e) => (fields[e.field] ??= e.message))
    return new ApiError(body.detail ?? error.message, error.response.status, body.code ?? `HTTP_${error.response.status}`, fields)
  }
  return new ApiError(error instanceof Error ? error.message : 'Something went wrong.', 0, 'UNKNOWN')
}

export const errorMessage = (error: unknown): string => toApiError(error).message
export const errorCode = (error: unknown): string => toApiError(error).code
