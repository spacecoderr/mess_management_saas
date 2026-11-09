import { z } from 'zod'

/** One letter + six digits, e.g. b423046@iiit-bh.ac.in (matches the backend rule; case-insensitive on input). */
export const COLLEGE_EMAIL = /^[a-z]\d{6}@iiit-bh\.ac\.in$/i

export const collegeEmail = z
  .string()
  .trim()
  .min(1, 'Enter your college email')
  .regex(COLLEGE_EMAIL, 'Use your IIIT Bhubaneswar email, like b423046@iiit-bh.ac.in')

export const anyEmail = z.string().trim().min(1, 'Enter your email').email('Enter a valid email address')

export const password = z
  .string()
  .min(8, 'At least 8 characters')
  .max(72, 'At most 72 characters')
  .regex(/[A-Za-z]/, 'Include at least one letter')
  .regex(/\d/, 'Include at least one number')

export const phone = z.string().trim().regex(/^[0-9+\- ]{10,15}$/, 'Enter a valid phone number')

export const otpCode = z.string().regex(/^\d{6}$/, 'Enter the 6-digit code')

/** "B423046" and the guessed batch year ("23" -> 2023) shown while registering. */
export function describeRoll(email: string) {
  const local = email.trim().split('@')[0] ?? ''
  if (!/^[a-z]\d{6}$/i.test(local)) return null
  return { rollNo: local.toUpperCase(), batchYear: 2000 + Number(local.slice(2, 4)) }
}
