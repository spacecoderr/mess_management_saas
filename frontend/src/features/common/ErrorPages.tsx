import { Link } from 'react-router-dom'
import { Compass, ShieldAlert } from 'lucide-react'
import { MeshBackground } from '@/components/ui/MeshBackground'
import { buttonClass } from '@/components/ui/Button'
import { ROLE_HOME, useAuthStore } from '@/store/authStore'

function Centered({ icon: Icon, code, title, text }: { icon: typeof Compass; code: string; title: string; text: string }) {
  const user = useAuthStore((s) => s.user)
  return (
    <>
      <MeshBackground />
      <main className="grid min-h-screen place-items-center px-4">
        <div className="glass-strong max-w-md p-10 text-center">
          <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-accent/15 text-accent">
            <Icon size={26} aria-hidden />
          </span>
          <p className="mt-4 font-display text-5xl font-bold text-heading">{code}</p>
          <h1 className="mt-1 text-xl font-bold">{title}</h1>
          <p className="mt-2 text-ink-soft">{text}</p>
          <Link to={user ? ROLE_HOME[user.role] : '/'} className={buttonClass('primary', 'mt-6')}>
            {user ? 'Back to my home' : 'Back to the homepage'}
          </Link>
        </div>
      </main>
    </>
  )
}

export const NotFoundPage = () => <Centered icon={Compass} code="404" title="Page not found" text="That page doesn't exist or has moved." />
export const ForbiddenPage = () => <Centered icon={ShieldAlert} code="403" title="Not allowed" text="Your account doesn't have access to that page." />
