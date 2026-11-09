import { RefreshCw } from 'lucide-react'
import { useLiveCounts } from '@/api/queries'
import { MEAL_META } from '@/components/meal/MealCard'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { ProgressRing } from '@/components/ui/ProgressRing'
import { Skeleton } from '@/components/ui/Skeleton'
import { EmptyState } from '@/components/ui/EmptyState'
import { UtensilsCrossed } from 'lucide-react'
import { ScanPanel } from './ScanPanel'

export default function ServeHome() {
  const live = useLiveCounts()
  const served = live.data?.reduce((n, m) => n + m.served, 0) ?? 0
  const booked = live.data?.reduce((n, m) => n + m.booked, 0) ?? 0

  return (
    <>
      <PageHeader
        title="Serve counter"
        subtitle="Live headcount for today. Refreshes every 15 seconds."
        action={
          <span className="glass flex items-center gap-2 !rounded-full px-4 py-2 text-sm font-semibold text-heading">
            <RefreshCw size={14} className={live.isFetching ? 'animate-spin' : ''} aria-hidden /> {served} of {booked} served today
          </span>
        }
      />
      {live.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[0, 1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-56" />
          ))}
        </div>
      ) : live.data && live.data.some((m) => m.menuId) ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {live.data.map((m) => {
            const meta = MEAL_META[m.mealType]
            const Icon = meta.icon
            return (
              <GlassCard key={m.mealType} className="p-5" aria-label={meta.label}>
                <div className="flex items-center gap-2 font-bold text-heading">
                  <Icon size={18} className="text-primary" aria-hidden /> {meta.label}
                </div>
                {m.menuId ? (
                  <>
                    <div className="my-4 flex justify-center">
                      <ProgressRing value={m.booked ? m.served / m.booked : 0} label={`${m.served} of ${m.booked} served`}>
                        <p className="font-display text-2xl font-bold text-heading">{m.served}</p>
                        <p className="text-[11px] text-ink-soft">of {m.booked}</p>
                      </ProgressRing>
                    </div>
                    <dl className="grid grid-cols-3 gap-2 text-center text-xs">
                      <div>
                        <dt className="text-ink-soft">Waiting</dt>
                        <dd className="text-lg font-bold text-heading">{m.remaining}</dd>
                      </div>
                      <div>
                        <dt className="text-ink-soft">No-show</dt>
                        <dd className="text-lg font-bold text-heading">{m.noShow}</dd>
                      </div>
                      <div>
                        <dt className="text-ink-soft">Cancelled</dt>
                        <dd className="text-lg font-bold text-heading">{m.cancelled}</dd>
                      </div>
                    </dl>
                  </>
                ) : (
                  <p className="mt-6 text-center text-sm text-ink-soft">No menu today</p>
                )}
              </GlassCard>
            )
          })}
        </div>
      ) : (
        <EmptyState icon={UtensilsCrossed} title="No meals scheduled today" text="Publish today's menu and bookings will appear here." />
      )}
      <ScanPanel />
    </>
  )
}
