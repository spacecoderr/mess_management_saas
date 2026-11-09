import { NavLink } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { Logo } from '@/components/ui/Logo'
import { Badge } from '@/components/ui/Badge'
import { cn } from '@/lib/cn'
import { initials } from '@/lib/format'
import { useLogout } from '@/hooks/useLogout'
import { useAuthStore } from '@/store/authStore'
import { NAV, type NavItem } from './nav'

export function NavList({ items, onNavigate }: { items: NavItem[]; onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="flex flex-col gap-1">
      {items.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          onClick={onNavigate}
          className={({ isActive }) =>
            cn(
              'flex items-center gap-3 rounded-2xl px-4 py-2.5 text-sm font-semibold transition-colors',
              isActive ? 'bg-primary text-on-primary shadow-glass' : 'text-ink-soft hover:bg-primary/10 hover:text-heading',
            )
          }
        >
          <Icon size={18} aria-hidden />
          {label}
        </NavLink>
      ))}
    </nav>
  )
}

export function UserCard() {
  const user = useAuthStore((s) => s.user)!
  const logout = useLogout()
  return (
    <div className="space-y-2 border-t pt-3" style={{ borderColor: 'var(--glass-border)' }}>
      <div className="flex items-center gap-3 rounded-2xl p-2">
        <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-accent font-bold text-on-accent">{initials(user.name)}</span>
        <div className="min-w-0">
          <p className="truncate text-sm font-bold text-heading">{user.name}</p>
          <Badge tone="primary" className="mt-0.5">
            {user.role.charAt(0) + user.role.slice(1).toLowerCase()}
          </Badge>
        </div>
      </div>
      <button
        type="button"
        onClick={logout}
        className="flex w-full items-center gap-3 rounded-2xl px-4 py-2.5 text-sm font-semibold text-ink-soft transition-colors hover:bg-danger/10 hover:text-danger"
      >
        <LogOut size={18} aria-hidden /> Log out
      </button>
    </div>
  )
}

export function Sidebar() {
  const role = useAuthStore((s) => s.user!.role)
  return (
    <div className="glass-strong sticky top-6 flex h-[calc(100vh-3rem)] flex-col gap-4 p-4">
      <Logo to="/app" className="px-2 pt-1" />
      {/* only the list scrolls: the user card and Log out stay visible on short screens */}
      <div className="-mr-2 min-h-0 flex-1 overflow-y-auto pr-2">
        <NavList items={NAV[role]} />
      </div>
      <UserCard />
    </div>
  )
}
