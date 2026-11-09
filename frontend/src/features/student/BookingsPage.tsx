import { useMemo, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { QrCode, Ticket } from 'lucide-react'
import { toast } from 'sonner'
import { useCancel, useMyBookings, useQr } from '@/api/student'
import { MEAL_META } from '@/components/meal/MealCard'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { addDays, dayLabel, isoDate } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { formatTime } from '@/lib/format'
import type { BookingStatus } from '@/types/api'

const TONE: Record<BookingStatus, 'primary' | 'success' | 'neutral' | 'danger'> = { BOOKED: 'primary', CONSUMED: 'success', CANCELLED: 'neutral', NO_SHOW: 'danger' }
const LABEL: Record<BookingStatus, string> = { BOOKED: 'Booked', CONSUMED: 'Eaten', CANCELLED: 'Cancelled', NO_SHOW: 'No-show' }

function QrDialog({ id, onClose }: { id: number | null; onClose: () => void }) {
  const qr = useQr(id)
  return (
    <Modal open={id !== null} onClose={onClose} title="Your meal pass">
      {qr.isLoading ? <Skeleton className="mx-auto h-56 w-56" /> : qr.data ? (
        <div className="space-y-3 text-center">
          <div className="mx-auto w-fit rounded-3xl bg-white p-4"><QRCodeSVG value={qr.data.payload} size={208} level="M" role="img" aria-label="QR code for your meal pass" /></div>
          <p className="font-bold text-heading">{MEAL_META[qr.data.mealType].label} · {dayLabel(qr.data.date)}</p>
          <p className="text-sm text-ink-soft">{qr.data.status === 'CONSUMED' ? 'This pass has been used.' : 'Show this at the counter. It works once.'}</p>
        </div>
      ) : <p className="text-sm text-danger">Couldn't load your pass.</p>}
    </Modal>
  )
}

export default function BookingsPage() {
  const today = useMemo(() => new Date(), [])
  const { data, isLoading } = useMyBookings(isoDate(addDays(today, -7)), isoDate(addDays(today, 13)))
  const cancel = useCancel()
  const [qrId, setQrId] = useState<number | null>(null)
  const todayIso = isoDate(today)
  const upcoming = (data ?? []).filter((b) => b.date >= todayIso && b.status !== 'CANCELLED')
  const past = (data ?? []).filter((b) => b.date < todayIso || b.status === 'CANCELLED').reverse()

  const row = (b: NonNullable<typeof data>[number]) => (
    <GlassCard key={b.id} hover={false} className="flex flex-wrap items-center justify-between gap-3 p-4">
      <div>
        <p className="font-bold text-heading">{MEAL_META[b.mealType].label} <span className="font-normal text-ink-soft">· {dayLabel(b.date)} · {formatTime(b.startTime)}</span></p>
        <p className="text-sm text-ink-soft">{b.dishes.join(', ') || 'Menu to be announced'}</p>
      </div>
      <div className="flex items-center gap-2">
        <Badge tone={TONE[b.status]}>{LABEL[b.status]}</Badge>
        {b.status !== 'CANCELLED' && b.status !== 'NO_SHOW' && <Button variant="ghost" className="!px-3 !py-2" onClick={() => setQrId(b.id)}><QrCode size={16} /> Pass</Button>}
        {b.cancellable && <Button variant="ghost" className="!px-3 !py-2" onClick={() => cancel.mutateAsync(b.id).then(() => toast.success('Booking cancelled')).catch((e) => toast.error(errorMessage(e)))}>Cancel</Button>}
      </div>
    </GlassCard>
  )

  return (
    <>
      <PageHeader title="My bookings" subtitle="Your upcoming meals and recent history." />
      {isLoading ? <Skeleton className="h-40" /> : (
        <div className="space-y-8">
          <section aria-labelledby="up"><h2 id="up" className="mb-3 text-xl font-bold">Upcoming</h2>
            {upcoming.length ? <div className="space-y-3">{upcoming.map(row)}</div> : <EmptyState icon={Ticket} title="Nothing booked yet" text="Head to the Menu page to book your meals." />}</section>
          {past.length > 0 && <section aria-labelledby="past"><h2 id="past" className="mb-3 text-xl font-bold">History</h2><div className="space-y-3">{past.map(row)}</div></section>}
        </div>
      )}
      <QrDialog id={qrId} onClose={() => setQrId(null)} />
    </>
  )
}
