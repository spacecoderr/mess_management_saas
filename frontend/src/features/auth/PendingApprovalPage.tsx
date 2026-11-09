import { Link, useLocation } from 'react-router-dom'
import { Hourglass } from 'lucide-react'
import { buttonClass } from '@/components/ui/Button'
import { AuthLayout } from './AuthLayout'

export default function PendingApprovalPage() {
  const email = (useLocation().state as { email?: string } | null)?.email
  return (
    <AuthLayout title="You're almost in">
      <div className="space-y-5 text-center">
        <span className="mx-auto grid h-16 w-16 place-items-center rounded-3xl bg-accent/15 text-accent">
          <Hourglass size={28} aria-hidden />
        </span>
        <p className="text-ink-soft">
          Your email {email && <strong className="text-heading">{email}</strong>} is verified and your account is waiting for approval from the mess office. This usually takes
          a day.
        </p>
        <p className="text-sm text-ink-soft">You can log in as soon as it's approved, and you'll be able to book your first meal right away.</p>
        <Link to="/login" className={buttonClass('primary', 'w-full')}>
          Go to log in
        </Link>
      </div>
    </AuthLayout>
  )
}
