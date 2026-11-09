import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Link, useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { Mail } from 'lucide-react'
import { toast } from 'sonner'
import { z } from 'zod'
import { requestOtp, resetPassword } from '@/api/auth'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { Input, PasswordInput } from '@/components/ui/Input'
import { OtpInput } from '@/components/ui/OtpInput'
import { useCountdown } from '@/hooks/useCountdown'
import { errorMessage } from '@/lib/errors'
import { anyEmail, otpCode, password } from '@/lib/validation'
import { AuthLayout } from './AuthLayout'

const resetSchema = z
  .object({ code: otpCode, password, confirm: z.string() })
  .refine((v) => v.password === v.confirm, { path: ['confirm'], message: "Passwords don't match" })
type ResetForm = z.infer<typeof resetSchema>

export default function ForgotPasswordPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState<string>()
  const timer = useCountdown(60)
  const emailForm = useForm<{ email: string }>({ resolver: zodResolver(z.object({ email: anyEmail })) })
  const resetForm = useForm<ResetForm>({ resolver: zodResolver(resetSchema), defaultValues: { code: '' } })

  const send = useMutation({
    mutationFn: (e: string) => requestOtp(e, 'RESET_PASSWORD'),
    onSuccess: (_d, e) => {
      setEmail(e.trim().toLowerCase())
      timer.start()
    },
  })
  const reset = useMutation({
    mutationFn: (v: ResetForm) => resetPassword(email!, v.code, v.password),
    onSuccess: () => {
      toast.success('Password updated. Log in with your new password.')
      navigate('/login', { replace: true })
    },
  })

  return (
    <AuthLayout
      title="Reset your password"
      subtitle={email ? undefined : "Enter your account email and we'll send a code."}
      footer={
        <Link to="/login" className="font-semibold text-accent underline-offset-4 hover:underline">
          Back to log in
        </Link>
      }
    >
      {!email ? (
        <form onSubmit={emailForm.handleSubmit((v) => send.mutate(v.email))} className="space-y-4" noValidate>
          <Input label="Email" type="email" icon={<Mail size={18} />} autoComplete="username" error={emailForm.formState.errors.email?.message} {...emailForm.register('email')} />
          {send.error && <Alert tone="error">{errorMessage(send.error)}</Alert>}
          <Button type="submit" loading={send.isPending} className="w-full">
            Send code
          </Button>
        </form>
      ) : (
        <form onSubmit={resetForm.handleSubmit((v) => reset.mutate(v))} className="space-y-4" noValidate>
          <Alert tone="info">
            If <strong>{email}</strong> has an account, a 6-digit code is on its way.
          </Alert>
          <div>
            <p className="mb-1.5 text-sm font-semibold text-heading">Code</p>
            <OtpInput value={resetForm.watch('code')} onChange={(c) => resetForm.setValue('code', c, { shouldValidate: resetForm.formState.isSubmitted })} invalid={!!resetForm.formState.errors.code} />
            {resetForm.formState.errors.code && (
              <p role="alert" className="mt-1.5 text-sm text-danger">
                {resetForm.formState.errors.code.message}
              </p>
            )}
          </div>
          <PasswordInput label="New password" autoComplete="new-password" hint="8+ characters with a letter and a number." error={resetForm.formState.errors.password?.message} {...resetForm.register('password')} />
          <PasswordInput label="Confirm new password" autoComplete="new-password" error={resetForm.formState.errors.confirm?.message} {...resetForm.register('confirm')} />
          {reset.error && <Alert tone="error">{errorMessage(reset.error)}</Alert>}
          <Button type="submit" loading={reset.isPending} className="w-full">
            Update password
          </Button>
          <div className="flex items-center justify-between text-sm">
            <button type="button" onClick={() => setEmail(undefined)} className="font-semibold text-ink-soft hover:text-heading">
              Use a different email
            </button>
            <button type="button" disabled={timer.running} onClick={() => send.mutate(email)} className="font-semibold text-accent disabled:text-ink-soft">
              {timer.running ? `Resend in ${timer.left}s` : 'Resend code'}
            </button>
          </div>
        </form>
      )}
    </AuthLayout>
  )
}
