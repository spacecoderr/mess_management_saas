import { Logo } from '@/components/ui/Logo'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { initials } from '@/lib/format'
import { useAuthStore } from '@/store/authStore'

export function Topbar() {
  const user = useAuthStore((s) => s.user)!
  return (
    <header className="mb-4 flex items-center justify-between gap-3 lg:justify-end">
      <Logo to="/app" className="lg:hidden" />
      <div className="flex items-center gap-3">
        <ThemeToggle />
        <div className="glass flex items-center gap-2 !rounded-full py-1 pl-1 pr-4">
          <span aria-hidden className="grid h-8 w-8 place-items-center rounded-full bg-accent text-xs font-bold text-on-accent">
            {initials(user.name)}
          </span>
          <span className="hidden text-sm font-semibold text-heading sm:block">{user.name}</span>
        </div>
      </div>
    </header>
  )
}
