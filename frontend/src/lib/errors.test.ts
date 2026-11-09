import { AxiosError, AxiosHeaders, type AxiosResponse } from 'axios'
import { ApiError, errorCode, errorMessage, toApiError } from './errors'

const failure = (status: number, data: unknown) => {
  const response = { status, data, statusText: '', headers: {}, config: { headers: new AxiosHeaders() } } as AxiosResponse
  return new AxiosError('Request failed', 'ERR_BAD_REQUEST', response.config, null, response)
}

describe('toApiError', () => {
  it('reads code and detail from the problem+json body', () => {
    const e = toApiError(failure(403, { status: 403, code: 'ACCOUNT_PENDING_APPROVAL', detail: 'Your account is waiting for admin approval.' }))
    expect(e).toBeInstanceOf(ApiError)
    expect(e.status).toBe(403)
    expect(e.code).toBe('ACCOUNT_PENDING_APPROVAL')
    expect(e.message).toBe('Your account is waiting for admin approval.')
    expect(e.isNetwork).toBe(false)
  })

  it('collects field errors from validation failures (first message per field)', () => {
    const e = toApiError(failure(400, { code: 'VALIDATION_FAILED', detail: 'Validation failed', errors: [{ field: 'password', message: 'too short' }, { field: 'password', message: 'other' }, { field: 'phone', message: 'bad' }] }))
    expect(e.fieldErrors).toEqual({ password: 'too short', phone: 'bad' })
  })

  it('falls back to an HTTP_<status> code when the body is not problem+json', () => {
    expect(errorCode(failure(502, '<html>Bad gateway</html>'))).toBe('HTTP_502')
  })

  it('explains a network failure in plain words', () => {
    const e = toApiError(new AxiosError('Network Error', 'ERR_NETWORK'))
    expect(e.isNetwork).toBe(true)
    expect(e.code).toBe('NETWORK')
    expect(e.message).toMatch(/can't reach the server/i)
  })

  it('treats a timeout as a (probably sleeping) server', () => {
    const e = toApiError(new AxiosError('timeout of 45000ms exceeded', 'ECONNABORTED'))
    expect(e.code).toBe('TIMEOUT')
    expect(e.message).toMatch(/waking up/i)
  })

  it('wraps unknown values and passes ApiErrors through untouched', () => {
    expect(errorMessage(new Error('boom'))).toBe('boom')
    expect(toApiError('weird').code).toBe('UNKNOWN')
    const original = new ApiError('x', 418, 'TEAPOT')
    expect(toApiError(original)).toBe(original)
  })
})
