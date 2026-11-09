import { useEffect, useState } from 'react'
import { Button } from '@/components/ui/Button'
import { Alert } from '@/components/ui/Alert'
import { OtpInput } from '@/components/ui/OtpInput'
import { useCountdown } from '@/hooks/useCountdown'
import { errorMessage } from '@/lib/errors'

interface Props {
  email: string
  /** Resolve to continue; reject (with an ApiError) to show the message under the boxes. */
  onVerify: (code: string) => Promise<unknown>
  onResend: () => Promise<unknown>
  onBack: () => void
  submitLabel?: string
}

/** "Enter the 6-digit code we emailed you": shared by registration and OTP login. */
export function OtpStep({ email, onVerify, onResend, onBack, submitLabel = 'Verify' }: Props) {
  const [code, setCode] = useState('')
  const [error, setError] = useState<string>()
  const [busy, setBusy] = useState(false)
  const [info, setInfo] = useState<string>()
  const timer = useCountdown(60)

  useEffect(() => timer.start(), [])   // eslint-disable-line react-hooks/exhaustive-deps -- a code was just sent, so the cooldown starts now

  const verify = async (value: string) => {
    if (busy) return
    setBusy(true)
    setError(undefined)
    try {
      await onVerify(value)
    } catch (e) {
      setError(errorMessage(e))
      setCode('')
    } finally {
      setBusy(false)
    }
  }

  const resend = async () => {
    setError(undefined)
    setInfo(undefined)
    try {
      await onResend()
      setInfo('A new code is on its way.')
      timer.start()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        if (code.length === 6) void verify(code)
      }}
      className="space-y-5"
    >
      <p className="text-sm text-ink-soft">
        We sent a 6-digit code to <strong className="text-heading">{email}</strong>. It expires in 5 minutes.
      </p>
      <OtpInput value={code} onChange={setCode} onComplete={(c) => void verify(c)} disabled={busy} invalid={!!error} />
      <div aria-live="polite">
        {error && <Alert tone="error">{error}</Alert>}
        {info && !error && <Alert tone="success">{info}</Alert>}
      </div>
      <Button type="submit" loading={busy} disabled={code.length !== 6} className="w-full">
        {submitLabel}
      </Button>
      <div className="flex items-center justify-between text-sm">
        <button type="button" onClick={onBack} className="font-semibold text-ink-soft underline-offset-4 hover:text-heading hover:underline">
          Use a different email
        </button>
        <button
          type="button"
          onClick={() => void resend()}
          disabled={timer.running}
          className="font-semibold text-accent underline-offset-4 hover:underline disabled:cursor-not-allowed disabled:text-ink-soft disabled:no-underline"
        >
          {timer.running ? `Resend code in ${timer.left}s` : 'Resend code'}
        </button>
      </div>
    </form>
  )
}
