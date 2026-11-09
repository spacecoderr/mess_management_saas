import type { LucideIcon } from 'lucide-react'
import { GlassCard } from './GlassCard'
import { Skeleton } from './Skeleton'
import { AnimatedNumber } from './AnimatedNumber'

interface Props {
  label: string
  icon: LucideIcon
  value?: number
  format?: (n: number) => string
  hint?: string
  loading?: boolean
  empty?: string
}

export function StatTile({ label, icon: Icon, value, format, hint, loading, empty = '—' }: Props) {
  return (
    <GlassCard className="p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-ink-soft">{label}</p>
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
          <Icon size={18} aria-hidden />
        </span>
      </div>
      <div className="mt-3 font-display text-3xl font-bold text-heading">
        {loading ? <Skeleton className="h-9 w-24" /> : value === undefined || value === null ? empty : <AnimatedNumber value={value} format={format} />}
      </div>
      {hint && <p className="mt-1 text-xs text-ink-soft">{hint}</p>}
    </GlassCard>
  )
}
