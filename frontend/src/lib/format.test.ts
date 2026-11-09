import { formatINR, formatMonth, formatTime, greeting, initials } from './format'

describe('format helpers', () => {
  it('formats rupees in Indian grouping', () => {
    expect(formatINR(3805)).toBe('₹3,805')
    expect(formatINR(125000)).toBe('₹1,25,000')
    expect(formatINR(60.5, true)).toBe('₹60.50')
  })

  it('formats 24h times for humans', () => {
    expect(formatTime('07:30:00')).toBe('7:30 AM')
    expect(formatTime('12:30:00')).toBe('12:30 PM')
    expect(formatTime('00:05:00')).toBe('12:05 AM')
    expect(formatTime('21:30:00')).toBe('9:30 PM')
  })

  it('formats a bill month', () => {
    expect(formatMonth('2026-10')).toMatch(/October 2026/)
  })

  it('makes avatar initials', () => {
    expect(initials('Ananya Behera')).toBe('AB')
    expect(initials('mess')).toBe('M')
    expect(initials('  Priya  Test Student ')).toBe('PT')
  })

  it('greets by time of day', () => {
    expect(greeting(new Date('2026-11-09T08:00:00'))).toBe('Good morning')
    expect(greeting(new Date('2026-11-09T14:00:00'))).toBe('Good afternoon')
    expect(greeting(new Date('2026-11-09T20:00:00'))).toBe('Good evening')
  })
})
