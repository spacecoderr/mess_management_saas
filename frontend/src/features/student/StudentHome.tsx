import { Link } from 'react-router-dom'
import { Megaphone, Pin, Receipt, UtensilsCrossed } from 'lucide-react'
import { useAnnouncements, useMyBills, useTodayMenus } from '@/api/queries'
import { MealCard } from '@/components/meal/MealCard'
import { Badge } from '@/components/ui/Badge'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { formatINR, formatMonth, greeting } from '@/lib/format'
import { useAuthStore } from '@/store/authStore'

const BILL_TONE = { PAID: 'success', UNPAID: 'accent', OVERDUE: 'danger' } as const

export default function StudentHome() {
  const user = useAuthStore((s) => s.user)!
  const menus = useTodayMenus()
  const bills = useMyBills()
  const notices = useAnnouncements()
  const bill = bills.data?.[0]

  return (
    <>
      <PageHeader title={`${greeting()}, ${user.name.split(' ')[0]}`} subtitle={user.rollNo ? `${user.rollNo} · ${user.hostelBlock ?? ''} ${user.roomNo ?? ''}`.trim() : "Here's what's on today."} />
      <div className="grid gap-6 lg:grid-cols-3">
        <section aria-labelledby="today-h" className="space-y-4 lg:col-span-2">
          <h2 id="today-h" className="text-xl font-bold">
            Today's meals
          </h2>
          {menus.isLoading ? (
            <div className="space-y-4">
              <Skeleton className="h-32" />
              <Skeleton className="h-32" />
            </div>
          ) : menus.data && menus.data.length > 0 ? (
            menus.data.map((m) => <MealCard key={m.id} menu={m} />)
          ) : (
            <EmptyState icon={UtensilsCrossed} title="No menu published yet" text="The mess office hasn't published today's menu. Check back soon." />
          )}
        </section>

        <div className="space-y-6">
          <GlassCard className="p-5" hover={false}>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold">Latest bill</h2>
              <Receipt size={18} className="text-primary" aria-hidden />
            </div>
            {bills.isLoading ? (
              <Skeleton className="mt-3 h-16" />
            ) : bill ? (
              <div className="mt-3">
                <p className="font-display text-3xl font-bold text-heading">{formatINR(bill.total)}</p>
                <p className="mt-1 flex items-center gap-2 text-sm text-ink-soft">
                  {formatMonth(bill.month)} <Badge tone={BILL_TONE[bill.status]}>{bill.status.toLowerCase()}</Badge>
                </p>
                <Link to="/app/bills" className="mt-3 inline-block text-sm font-semibold text-accent underline-offset-4 hover:underline">
                  View bills
                </Link>
              </div>
            ) : (
              <p className="mt-3 text-sm text-ink-soft">No bills yet. Your first bill appears after the month ends.</p>
            )}
          </GlassCard>

          <GlassCard className="p-5" hover={false}>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold">Notice board</h2>
              <Megaphone size={18} className="text-primary" aria-hidden />
            </div>
            {notices.isLoading ? (
              <Skeleton className="mt-3 h-24" />
            ) : notices.data && notices.data.length > 0 ? (
              <ul className="mt-3 space-y-3">
                {notices.data.slice(0, 3).map((n) => (
                  <li key={n.id}>
                    <p className="flex items-center gap-1.5 text-sm font-bold text-heading">
                      {n.pinned && <Pin size={13} className="text-accent" aria-label="pinned" />}
                      {n.title}
                    </p>
                    <p className="text-sm text-ink-soft">{n.body}</p>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-3 text-sm text-ink-soft">Nothing new right now.</p>
            )}
          </GlassCard>
        </div>
      </div>
    </>
  )
}
