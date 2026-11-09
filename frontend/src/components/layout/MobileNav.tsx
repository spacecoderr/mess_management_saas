import { useEffect } from 'react'
import { NavLink } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { Menu, X } from 'lucide-react'
import { Logo } from '@/components/ui/Logo'
import { cn } from '@/lib/cn'
import { useAuthStore } from '@/store/authStore'
import { NAV } from './nav'
import { NavList, UserCard } from './Sidebar'

/** Bottom tab bar on phones: the first four destinations plus "More", which opens the full drawer. */
export function BottomNav({ onMore }: { onMore: () => void }) {
  const role = useAuthStore((s) => s.user!.role)
  const items = NAV[role].slice(0, 4)
  return (
    <nav aria-label="Primary" className="glass-strong fixed inset-x-3 bottom-3 z-30 flex items-center justify-around !rounded-full px-2 py-1.5 lg:hidden">
      {items.map(({ to, label, icon: Icon }) => (
        <NavLink
          key={to}
          to={to}
          className={({ isActive }) =>
            cn('flex min-w-[3.6rem] flex-col items-center gap-0.5 rounded-full px-2 py-1.5 text-[11px] font-semibold', isActive ? 'bg-primary text-on-primary' : 'text-ink-soft')
          }
        >
          <Icon size={18} aria-hidden />
          <span className="max-w-[4.5rem] truncate">{label}</span>
        </NavLink>
      ))}
      <button type="button" onClick={onMore} className="flex min-w-[3.6rem] flex-col items-center gap-0.5 rounded-full px-2 py-1.5 text-[11px] font-semibold text-ink-soft">
        <Menu size={18} aria-hidden />
        More
      </button>
    </nav>
  )
}

export function Drawer({ open, onClose }: { open: boolean; onClose: () => void }) {
  const role = useAuthStore((s) => s.user!.role)
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Menu">
          <motion.div className="absolute inset-0 bg-black/40 backdrop-blur-sm" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose} />
          <motion.div
            className="glass-strong absolute inset-y-3 left-3 flex w-72 max-w-[85vw] flex-col gap-4 p-4"
            initial={{ x: '-110%' }}
            animate={{ x: 0 }}
            exit={{ x: '-110%' }}
            transition={{ type: 'spring', stiffness: 320, damping: 34 }}
          >
            <div className="flex items-center justify-between px-2 pt-1">
              <Logo to="/app" />
              <button type="button" onClick={onClose} aria-label="Close menu" className="rounded-full p-2 text-ink-soft hover:text-heading">
                <X size={20} />
              </button>
            </div>
            <div className="-mr-2 min-h-0 flex-1 overflow-y-auto pr-2">
              <NavList items={NAV[role]} onNavigate={onClose} />
            </div>
            <UserCard />
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  )
}
