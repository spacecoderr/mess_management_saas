import { useMemo, useState } from 'react'
import { CalendarCheck, ChevronLeft, ChevronRight } from 'lucide-react'
import { toast } from 'sonner'
import { useBook, useBookMany, useCancel, useMenus, useMyBookings } from '@/api/student'
import { MealCard } from '@/components/meal/MealCard'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { addDays, dayLabel, isoDate } from '@/lib/dates'
import { cn } from '@/lib/cn'
import { errorMessage } from '@/lib/errors'
import type { DailyMenu } from '@/types/api'

export default function MenuPage() {
  const [offset, setOffset] = useState(0)   // weeks from today
  const start = useMemo(() => addDays(new Date(), offset * 7), [offset])
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => isoDate(addDays(start, i))), [start])
  const [selected, setSelected] = useState(days[0]!)
  const day = days.includes(selected) ? selected : days[0]!

  const menus = useMenus(days[0]!, days[6]!)
  const bookings = useMyBookings(days[0]!, days[6]!)
  const book = useBook(), bookMany = useBookMany(), cancel = useCancel()

  const bookingFor = (m: DailyMenu) => bookings.data?.find((b) => b.dailyMenuId === m.id && b.status !== 'CANCELLED')
  const dayMenus = (menus.data ?? []).filter((m) => m.date === day)
  const bookable = (menus.data ?? []).filter((m) => m.bookingOpen && !bookingFor(m))

  const run = async (p: Promise<unknown>, ok: string) => {
    try { await p; toast.success(ok) } catch (e) { toast.error(errorMessage(e)) }
  }
  const bookAll = async () => {
    try {
      const r = await bookMany.mutateAsync(bookable.map((m) => ({ date: m.date, mealType: m.mealType })))
      toast.success(`Booked ${r.booked.length} meal${r.booked.length === 1 ? '' : 's'}${r.skipped.length ? `, ${r.skipped.length} skipped` : ''}`)
    } catch (e) { toast.error(errorMessage(e)) }
  }

  return (
    <>
      <PageHeader title="Menu & booking" subtitle="Book before the cutoff shown on each meal." action={
        <Button variant="accent" onClick={bookAll} loading={bookMany.isPending} disabled={bookable.length === 0}><CalendarCheck size={16} /> Book all open meals ({bookable.length})</Button>} />
      <div className="mb-5 flex items-center gap-2">
        <button type="button" aria-label="Previous week" disabled={offset <= 0} onClick={() => setOffset(offset - 1)} className="btn-ghost !p-2.5"><ChevronLeft size={18} /></button>
        <div role="tablist" aria-label="Day" className="flex flex-1 gap-2 overflow-x-auto pb-1">
          {days.map((d) => (
            <button key={d} role="tab" aria-selected={d === day} onClick={() => setSelected(d)}
              className={cn('shrink-0 rounded-2xl border px-4 py-2 text-sm font-semibold transition', d === day ? 'border-primary bg-primary text-on-primary' : 'text-ink-soft hover:text-heading')}
              style={d === day ? undefined : { background: 'var(--glass-bg)', borderColor: 'var(--glass-border)' }}>{dayLabel(d)}</button>
          ))}
        </div>
        <button type="button" aria-label="Next week" onClick={() => setOffset(offset + 1)} className="btn-ghost !p-2.5"><ChevronRight size={18} /></button>
      </div>

      {menus.isLoading ? <div className="space-y-4"><Skeleton className="h-36" /><Skeleton className="h-36" /></div>
        : dayMenus.length === 0 ? <EmptyState icon={CalendarCheck} title="No menu for this day yet" text="Menus appear here once the mess office publishes them." />
        : <div className="grid gap-4 lg:grid-cols-2">
            {dayMenus.map((m) => {
              const b = bookingFor(m)
              return (
                <MealCard key={m.id} menu={m} action={
                  b ? (
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-sm font-bold text-success">Booked ✓ {b.status === 'CONSUMED' && '· Eaten'}</span>
                      {b.cancellable && <Button variant="ghost" className="!py-2" loading={cancel.isPending && cancel.variables === b.id} onClick={() => run(cancel.mutateAsync(b.id), 'Booking cancelled')}>Cancel</Button>}
                    </div>
                  ) : m.bookingOpen ? (
                    <Button className="w-full" loading={book.isPending && book.variables?.mealType === m.mealType && book.variables?.date === m.date} onClick={() => run(book.mutateAsync({ date: m.date, mealType: m.mealType }), 'Meal booked')}>Book this meal</Button>
                  ) : <p className="text-sm text-ink-soft">Booking for this meal has closed.</p>
                } />
              )
            })}
          </div>}
    </>
  )
}
