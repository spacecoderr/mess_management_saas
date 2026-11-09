import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { CheckCircle2, Mail } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { registerStudent, requestOtp, verifyOtp } from '@/api/auth'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input, PasswordInput } from '@/components/ui/Input'
import { StepIndicator } from '@/components/ui/StepIndicator'
import { ApiError, toApiError } from '@/lib/errors'
import { cn } from '@/lib/cn'
import { collegeEmail, describeRoll, password, phone } from '@/lib/validation'
import { AuthLayout } from './AuthLayout'
import { OtpStep } from './OtpStep'

const STEPS = ['College email', 'Verify', 'Your details']
const BLOCKS = ['Boys Hostel A', 'Boys Hostel B', 'Girls Hostel A', 'Girls Hostel B']

const emailSchema = z.object({ email: collegeEmail })

const profileSchema = z
  .object({
    name: z.string().trim().min(2, 'Enter your full name').max(120),
    phone,
    hostelBlock: z.string().trim().min(1, 'Enter your hostel block').max(30),
    roomNo: z.string().trim().min(1, 'Enter your room number').max(20),
    dietPref: z.enum(['VEG', 'NON_VEG', 'JAIN']),
    password,
    confirm: z.string(),
  })
  .refine((v) => v.password === v.confirm, { path: ['confirm'], message: "Passwords don't match" })
type ProfileForm = z.infer<typeof profileSchema>

const DIET = [
  { value: 'VEG', label: 'Vegetarian' },
  { value: 'NON_VEG', label: 'Non-veg' },
  { value: 'JAIN', label: 'Jain' },
] as const

function EmailStep({ onSent, notice }: { onSent: (email: string) => void; notice?: string }) {
  const form = useForm<{ email: string }>({ resolver: zodResolver(emailSchema) })
  const send = useMutation({ mutationFn: (v: { email: string }) => requestOtp(v.email, 'REGISTER'), onSuccess: (_d, v) => onSent(v.email.trim().toLowerCase()) })
  const roll = describeRoll(form.watch('email') ?? '')
  const err = send.error ? toApiError(send.error) : null

  return (
    <form onSubmit={form.handleSubmit((v) => send.mutate(v))} className="space-y-4" noValidate>
      {notice && <Alert tone="info">{notice}</Alert>}
      <Input
        label="College email"
        type="email"
        autoComplete="username"
        icon={<Mail size={18} />}
        placeholder="b423046@iiit-bh.ac.in"
        hint={roll ? `Roll number ${roll.rollNo} · batch of ${roll.batchYear}` : 'Only @iiit-bh.ac.in student emails can register.'}
        error={form.formState.errors.email?.message}
        {...form.register('email')}
      />
      {err && (
        <Alert tone="error">
          {err.message}{' '}
          {err.code === 'EMAIL_ALREADY_REGISTERED' && (
            <Link to="/login" className="font-semibold underline">
              Log in instead
            </Link>
          )}
        </Alert>
      )}
      <Button type="submit" loading={send.isPending} className="w-full">
        Send verification code
      </Button>
    </form>
  )
}

function DetailsStep({ email, registrationToken, onExpired }: { email: string; registrationToken: string; onExpired: () => void }) {
  const navigate = useNavigate()
  const roll = describeRoll(email)
  const form = useForm<ProfileForm>({ resolver: zodResolver(profileSchema), defaultValues: { dietPref: 'VEG' } })
  const { errors } = form.formState
  const register = useMutation({
    mutationFn: (v: ProfileForm) =>
      registerStudent({ registrationToken, name: v.name, phone: v.phone, hostelBlock: v.hostelBlock, roomNo: v.roomNo, dietPref: v.dietPref, password: v.password }),
    onSuccess: () => {
      toast.success('Account created! An admin will approve it shortly.')
      navigate('/pending-approval', { replace: true, state: { email } })
    },
    onError: (e) => {
      const err = toApiError(e)
      if (err.code === 'REGISTRATION_TOKEN_INVALID') return onExpired()
      Object.entries(err.fieldErrors).forEach(([field, message]) => form.setError(field as keyof ProfileForm, { message }))
    },
  })
  const err = register.error ? (toApiError(register.error) as ApiError) : null

  return (
    <form onSubmit={form.handleSubmit((v) => register.mutate(v))} className="space-y-4" noValidate>
      <div className="flex items-center gap-2 rounded-2xl bg-success/10 px-4 py-3 text-sm font-semibold text-success">
        <CheckCircle2 size={18} aria-hidden /> {email} verified
        {roll && <span className="ml-auto text-ink-soft">{roll.rollNo}</span>}
      </div>
      <Input label="Full name" autoComplete="name" error={errors.name?.message} {...form.register('name')} />
      <Input label="Phone" type="tel" autoComplete="tel" placeholder="98765 43210" error={errors.phone?.message} {...form.register('phone')} />
      <div className="grid gap-4 sm:grid-cols-2">
        <div>
          <Input label="Hostel block" list="blocks" error={errors.hostelBlock?.message} {...form.register('hostelBlock')} />
          <datalist id="blocks">
            {BLOCKS.map((b) => (
              <option key={b} value={b} />
            ))}
          </datalist>
        </div>
        <Input label="Room number" error={errors.roomNo?.message} {...form.register('roomNo')} />
      </div>

      <fieldset>
        <legend className="mb-1.5 text-sm font-semibold text-heading">Food preference</legend>
        <div className="grid grid-cols-3 gap-2">
          {DIET.map((d) => (
            <label key={d.value} className="cursor-pointer">
              <input type="radio" value={d.value} className="peer sr-only" {...form.register('dietPref')} />
              <span
                className={cn(
                  'block rounded-2xl border px-3 py-2.5 text-center text-sm font-semibold text-ink-soft transition',
                  'peer-checked:border-primary peer-checked:bg-primary peer-checked:text-on-primary peer-focus-visible:ring-2 peer-focus-visible:ring-accent/70',
                )}
                style={{ background: 'var(--field-bg)', borderColor: 'var(--glass-border)' }}
              >
                {d.label}
              </span>
            </label>
          ))}
        </div>
      </fieldset>

      <PasswordInput label="Create a password" autoComplete="new-password" hint="8+ characters with a letter and a number." error={errors.password?.message} {...form.register('password')} />
      <PasswordInput label="Confirm password" autoComplete="new-password" error={errors.confirm?.message} {...form.register('confirm')} />

      {err && Object.keys(err.fieldErrors).length === 0 && <Alert tone="error">{err.message}</Alert>}
      <Button type="submit" loading={register.isPending} className="w-full">
        Create account
      </Button>
    </form>
  )
}

export default function RegisterPage() {
  const [step, setStep] = useState(0)
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [notice, setNotice] = useState<string>()

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Verify your college email, then tell us a little about you."
      footer={
        <>
          Already registered?{' '}
          <Link to="/login" className="font-semibold text-accent underline-offset-4 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <div className="space-y-6">
        <StepIndicator steps={STEPS} current={step} />
        {step === 0 && (
          <EmailStep
            notice={notice}
            onSent={(e) => {
              setEmail(e)
              setNotice(undefined)
              setStep(1)
            }}
          />
        )}
        {step === 1 && (
          <OtpStep
            email={email}
            submitLabel="Verify email"
            onBack={() => setStep(0)}
            onResend={() => requestOtp(email, 'REGISTER')}
            onVerify={async (code) => {
              const res = await verifyOtp(email, 'REGISTER', code)
              if (!res.registrationToken) throw new Error('Verification failed. Please try again.')
              setToken(res.registrationToken)
              setStep(2)
            }}
          />
        )}
        {step === 2 && (
          <DetailsStep
            email={email}
            registrationToken={token}
            onExpired={() => {
              setNotice('Your email verification expired. Please verify again.')
              setStep(0)
            }}
          />
        )}
      </div>
    </AuthLayout>
  )
}
