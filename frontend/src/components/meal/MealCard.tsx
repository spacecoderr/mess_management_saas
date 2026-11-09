import type { ReactNode } from 'react'
import { Coffee, Moon, Sun, Sunrise, type LucideIcon } from 'lucide-react'
import { Badge } from '@/components/ui/Badge'
import { formatINR, formatTime } from '@/lib/format'
import { cn } from '@/lib/cn'
import type { DailyMenu, MealType } from '@/types/api'

/** Each meal has its own time-of-day tint: sunrise peach, midday sage, tea-time ivory, evening forest. */
export const MEAL_META: Record<MealType, { label: string; icon: LucideIcon; tint: string }> = {
  BREAKFAST: { label: 'Breakfast', icon: Sunrise, tint: 'from-terracotta-100 to-white/40 dark:from-terracotta-700/30 dark:to-white/5' },
  LUNCH: { label: 'Lunch', icon: Sun, tint: 'from-sage-200 to-white/40 dark:from-sage-600/30 dark:to-white/5' },
  SNACKS: { label: 'Snacks', icon: Coffee, tint: 'from-ivory-300 to-white/40 dark:from-ivory-300/15 dark:to-white/5' },
  DINNER: { label: 'Dinner', icon: Moon, tint: 'from-forest-100 to-white/40 dark:from-forest-500/25 dark:to-white/5' },
}

export function MealCard({ menu, action }: { menu: DailyMenu; action?: ReactNode }) {
  const meta = MEAL_META[menu.mealType]
  const Icon = meta.icon
  const closes = new Date(menu.bookingClosesAt).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' })
  return (
    <article className={cn('rounded-3xl border bg-gradient-to-br p-5', meta.tint)} style={{ borderColor: 'var(--glass-border)' }} aria-label={meta.label}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <span className="grid h-10 w-10 place-items-center rounded-2xl bg-primary/10 text-primary">
            <Icon size={20} aria-hidden />
          </span>
          <div>
            <h3 className="text-lg font-bold leading-tight">{meta.label}</h3>
            <p className="text-xs font-semibold text-ink-soft">
              {formatTime(menu.startTime)} – {formatTime(menu.endTime)} · {formatINR(menu.price)}
            </p>
          </div>
        </div>
        <Badge tone={menu.bookingOpen ? 'success' : 'neutral'}>{menu.bookingOpen ? `Open till ${closes}` : 'Booking closed'}</Badge>
      </div>
      <ul className="mt-4 flex flex-wrap gap-2">
        {menu.items.map((d) => (
          <li key={d.id} className="flex items-center gap-1.5 rounded-full bg-white/40 px-3 py-1 text-sm font-medium text-ink dark:bg-white/10">
            <span aria-label={d.veg ? 'vegetarian' : 'non-vegetarian'} className={cn('h-2 w-2 rounded-full', d.veg ? 'bg-success' : 'bg-accent')} />
            {d.name}
          </li>
        ))}
      </ul>
      {action && <div className="mt-4">{action}</div>}
    </article>
  )
}
