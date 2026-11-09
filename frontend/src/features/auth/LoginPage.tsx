import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Mail } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { loginWithPassword, requestOtp, verifyOtp } from '@/api/auth'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input, PasswordInput } from '@/components/ui/Input'
import { Tabs } from '@/components/ui/Tabs'
import { DEMO_ACCOUNTS, SHOW_DEMO } from '@/lib/demo'
import { errorCode, errorMessage } from '@/lib/errors'
import { anyEmail, collegeEmail } from '@/lib/validation'
import { useAuthStore } from '@/store/authStore'
import type { AuthResponse } from '@/types/api'
import { AuthLayout } from './AuthLayout'
import { OtpStep } from './OtpStep'

const passwordSchema = z.object({ email: anyEmail, password: z.string().min(1, 'Enter your password') })
type PasswordForm = z.infer<typeof passwordSchema>
const otpEmailSchema = z.object({ email: collegeEmail })

type Mode = 'password' | 'otp'

/** Stores the session. The redirect afterwards (to the page they came from, or their home) is done by PublicOnlyRoute. */
function useFinishLogin() {
  const setSession = useAuthStore((s) => s.setSession)
  return (auth: AuthResponse) => {
    setSession(auth)
    toast.success(`Welcome, ${auth.user.name.split(' ')[0]}!`)
  }
}

function PasswordLogin({ fill }: { fill: { email: string; password: string; n: number } | null }) {
  const finish = useFinishLogin()
  const form = useForm<PasswordForm>({ resolver: zodResolver(passwordSchema), values: fill ? { email: fill.email, password: fill.password } : undefined })
  const login = useMutation({ mutationFn: (v: PasswordForm) => loginWithPassword(v.email, v.password), onSuccess: finish })
  const code = login.error ? errorCode(login.error) : null

  return (
    <form onSubmit={form.handleSubmit((v) => login.mutate(v))} className="space-y-4" noValidate>
      <Input label="Email" type="email" autoComplete="username" placeholder="b423046@iiit-bh.ac.in" error={form.formState.errors.email?.message} {...form.register('email')} />
      <PasswordInput label="Password" autoComplete="current-password" error={form.formState.errors.password?.message} {...form.register('password')} />
      {login.error && (
        <Alert tone={code === 'ACCOUNT_PENDING_APPROVAL' ? 'info' : 'error'}>
          {errorMessage(login.error)}{' '}
          {code === 'ACCOUNT_PENDING_APPROVAL' && (
            <Link to="/pending-approval" className="font-semibold underline">
              What happens next?
            </Link>
          )}
        </Alert>
      )}
      <Button type="submit" loading={login.isPending} className="w-full">
        Log in
      </Button>
      <p className="text-center text-sm">
        <Link to="/forgot-password" className="font-semibold text-accent underline-offset-4 hover:underline">
          Forgot your password?
        </Link>
      </p>
    </form>
  )
}

function OtpLogin() {
  const finish = useFinishLogin()
  const [email, setEmail] = useState<string>()
  const form = useForm<{ email: string }>({ resolver: zodResolver(otpEmailSchema) })
  const send = useMutation({ mutationFn: (v: { email: string }) => requestOtp(v.email, 'LOGIN'), onSuccess: (_d, v) => setEmail(v.email.trim().toLowerCase()) })

  if (email) {
    return (
      <OtpStep
        email={email}
        submitLabel="Log in"
        onBack={() => setEmail(undefined)}
        onResend={() => requestOtp(email, 'LOGIN')}
        onVerify={async (code) => {
          const res = await verifyOtp(email, 'LOGIN', code)
          if (res.auth) finish(res.auth)
        }}
      />
    )
  }
  return (
    <form onSubmit={form.handleSubmit((v) => send.mutate(v))} className="space-y-4" noValidate>
      <Input
        label="College email"
        type="email"
        autoComplete="username"
        icon={<Mail size={18} />}
        placeholder="b423046@iiit-bh.ac.in"
        hint="We'll email you a one-time code. No password needed."
        error={form.formState.errors.email?.message}
        {...form.register('email')}
      />
      {send.error && <Alert tone="error">{errorMessage(send.error)}</Alert>}
      <Button type="submit" loading={send.isPending} className="w-full">
        Email me a code
      </Button>
    </form>
  )
}

export default function LoginPage() {
  const [mode, setMode] = useState<Mode>('password')
  const [fill, setFill] = useState<{ email: string; password: string; n: number } | null>(null)
  const expired = useAuthStore((s) => s.sessionExpired)

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Log in to book meals, scan in and pay your bill."
      footer={
        <>
          New student?{' '}
          <Link to="/register" className="font-semibold text-accent underline-offset-4 hover:underline">
            Create your account
          </Link>
        </>
      }
    >
      <div className="space-y-5">
        {expired && <Alert tone="info">Your session expired. Please log in again.</Alert>}
        <Tabs
          label="Login method"
          value={mode}
          onChange={setMode}
          options={[
            { value: 'password', label: 'Password' },
            { value: 'otp', label: 'Email code' },
          ]}
        />
        {mode === 'password' ? <PasswordLogin fill={fill} /> : <OtpLogin />}

        {SHOW_DEMO && (
          <div className="rounded-2xl border border-dashed p-3" style={{ borderColor: 'var(--glass-border)' }}>
            <p className="mb-2 text-xs font-bold uppercase tracking-wide text-ink-soft">Demo accounts</p>
            <div className="flex flex-wrap gap-2">
              {DEMO_ACCOUNTS.map((a) => (
                <button
                  key={a.role}
                  type="button"
                  onClick={() => {
                    setMode('password')
                    setFill({ email: a.email, password: a.password, n: Date.now() })
                  }}
                  className="rounded-full bg-primary/10 px-3 py-1.5 text-sm font-semibold text-primary transition hover:bg-primary/20"
                >
                  {a.role}
                </button>
              ))}
            </div>
            <p className="mt-2 text-xs text-ink-soft">Click a role to fill the form, then press Log in.</p>
          </div>
        )}
      </div>
    </AuthLayout>
  )
}
