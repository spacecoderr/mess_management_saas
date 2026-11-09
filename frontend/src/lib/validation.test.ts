import { collegeEmail, describeRoll, password, phone, otpCode, COLLEGE_EMAIL } from './validation'

describe('college email', () => {
  it.each(['b423046@iiit-bh.ac.in', 'B423046@IIIT-BH.AC.IN', '  b423046@iiit-bh.ac.in '])('accepts %s', (e) => {
    expect(collegeEmail.safeParse(e).success).toBe(true)
  })

  it.each(['someone@gmail.com', 'b42304@iiit-bh.ac.in', '423046@iiit-bh.ac.in', 'b423046@iiit-bh.ac.in.evil.com', 'xb423046@iiit-bh.ac.in', '', 'b423046@mail.iiit-bh.ac.in'])('rejects %j', (e) => {
    expect(collegeEmail.safeParse(e).success).toBe(false)
  })

  it('uses the same rule as the backend (one letter + six digits)', () => {
    expect(COLLEGE_EMAIL.test('a000000@iiit-bh.ac.in')).toBe(true)
    expect(COLLEGE_EMAIL.test('ab00000@iiit-bh.ac.in')).toBe(false)
  })
})

describe('describeRoll', () => {
  it('derives the roll number and batch year from the email', () => {
    expect(describeRoll('b423046@iiit-bh.ac.in')).toEqual({ rollNo: 'B423046', batchYear: 2023 })
    expect(describeRoll('b424777@iiit-bh.ac.in')).toEqual({ rollNo: 'B424777', batchYear: 2024 })
  })
  it('returns null for anything that is not a student email', () => {
    expect(describeRoll('admin@thali.local')).toBeNull()
    expect(describeRoll('b4')).toBeNull()
    expect(describeRoll('')).toBeNull()
  })
})

describe('password rules', () => {
  it('needs 8+ chars with a letter and a number', () => {
    expect(password.safeParse('Passw0rd1').success).toBe(true)
    expect(password.safeParse('short1').success).toBe(false)
    expect(password.safeParse('onlyletters').success).toBe(false)
    expect(password.safeParse('12345678').success).toBe(false)
    expect(password.safeParse('a1'.repeat(40)).success).toBe(false)   // > 72
  })
})

describe('phone and otp', () => {
  it('validates phone numbers', () => {
    expect(phone.safeParse('9876543210').success).toBe(true)
    expect(phone.safeParse('+91 98765-43210').success).toBe(true)
    expect(phone.safeParse('12345').success).toBe(false)
    expect(phone.safeParse('call-me-maybe').success).toBe(false)
  })
  it('requires exactly six digits for the code', () => {
    expect(otpCode.safeParse('123456').success).toBe(true)
    expect(otpCode.safeParse('12345').success).toBe(false)
    expect(otpCode.safeParse('12345a').success).toBe(false)
  })
})
