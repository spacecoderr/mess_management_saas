import { useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { CheckCircle2, CreditCard, Receipt, Smartphone } from 'lucide-react'
import { useBillDetail, useCheckout, useConfirmPayment } from '@/api/student'
import { useMyBills } from '@/api/queries'
import { MEAL_META } from '@/components/meal/MealCard'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { dayLabel } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { formatINR, formatMonth } from '@/lib/format'
import type { Bill, Payment } from '@/types/api'

const TONE = { PAID: 'success', UNPAID: 'accent', OVERDUE: 'danger' } as const

function DetailDialog({ id, onClose }: { id: number | null; onClose: () => void }) {
  const q = useBillDetail(id)
  const b = q.data
  return (
    <Modal open={id !== null} onClose={onClose} title={b ? `${formatMonth(b.month)} bill` : 'Bill'}>
      {!b ? <Skeleton className="h-40" /> : (
        <div className="space-y-4">
          <dl className="space-y-1 text-sm">
            <div className="flex justify-between"><dt>{b.mealCount} meals</dt><dd>{formatINR(b.mealCharges, true)}</dd></div>
            <div className="flex justify-between"><dt>Fixed monthly charge</dt><dd>{formatINR(b.fixedCharges, true)}</dd></div>
            <div className="flex justify-between border-t pt-2 text-base font-bold text-heading" style={{ borderColor: 'var(--glass-border)' }}><dt>Total</dt><dd>{formatINR(b.total, true)}</dd></div>
          </dl>
          {b.lines && b.lines.length > 0 && (
            <ul className="max-h-56 space-y-1 overflow-y-auto text-sm" aria-label="Meals in this bill">
              {b.lines.map((l, i) => <li key={i} className="flex justify-between text-ink-soft"><span>{dayLabel(l.date)} · {MEAL_META[l.mealType].label}{l.status === 'NO_SHOW' && ' (no-show)'}</span><span>{formatINR(l.price)}</span></li>)}
            </ul>
          )}
        </div>
      )}
    </Modal>
  )
}

/** Mock checkout: choose UPI or card, "pay", then confirm. No real money moves. */
function PayDialog({ bill, onClose }: { bill: Bill | null; onClose: () => void }) {
  const checkout = useCheckout(), confirm = useConfirmPayment()
  const [pay, setPay] = useState<Payment | null>(null)
  const [done, setDone] = useState(false)
  const close = () => { setPay(null); setDone(false); checkout.reset(); confirm.reset(); onClose() }
  const err = checkout.error ?? confirm.error
  return (
    <Modal open={!!bill} onClose={close} title={done ? 'Payment successful' : 'Pay your bill'}>
      {bill && (done ? (
        <div className="space-y-4 text-center"><CheckCircle2 className="mx-auto text-success" size={56} aria-hidden />
          <p className="font-bold text-heading">{formatINR(bill.total, true)} paid for {formatMonth(bill.month)}</p>
          <p className="text-sm text-ink-soft">Reference {pay?.txnRef}</p><Button className="w-full" onClick={close}>Done</Button></div>
      ) : !pay ? (
        <div className="space-y-3">
          <p className="text-sm text-ink-soft">Total due <strong className="text-heading">{formatINR(bill.total, true)}</strong> for {formatMonth(bill.month)}. This is a demo gateway; no real money moves.</p>
          {err && <Alert tone="error">{errorMessage(err)}</Alert>}
          <Button className="w-full" loading={checkout.isPending} onClick={() => checkout.mutate({ billId: bill.id, method: 'UPI' }, { onSuccess: setPay })}><Smartphone size={16} /> Pay with UPI</Button>
          <Button variant="ghost" className="w-full" loading={checkout.isPending} onClick={() => checkout.mutate({ billId: bill.id, method: 'CARD' }, { onSuccess: setPay })}><CreditCard size={16} /> Pay with card</Button>
        </div>
      ) : (
        <div className="space-y-4 text-center">
          {pay.method === 'UPI' && pay.gatewayPayload && <div className="mx-auto w-fit rounded-2xl bg-white p-3"><QRCodeSVG value={pay.gatewayPayload} size={160} role="img" aria-label="UPI payment QR code" /></div>}
          <p className="text-sm text-ink-soft">{pay.method === 'UPI' ? 'Scan with any UPI app, then confirm below.' : 'Demo card checkout. Press pay to complete.'}</p>
          {err && <Alert tone="error">{errorMessage(err)}</Alert>}
          <Button className="w-full" loading={confirm.isPending} onClick={() => confirm.mutate(pay.id, { onSuccess: () => setDone(true) })}>{pay.method === 'UPI' ? "I've paid" : `Pay ${formatINR(pay.amount, true)}`}</Button>
        </div>
      ))}
    </Modal>
  )
}

export default function BillsPage() {
  const bills = useMyBills()
  const [detail, setDetail] = useState<number | null>(null)
  const [paying, setPaying] = useState<Bill | null>(null)
  return (
    <>
      <PageHeader title="Bills" subtitle="Your monthly mess bills." />
      {bills.isLoading ? <Skeleton className="h-40" /> : bills.data?.length ? (
        <div className="grid gap-4 md:grid-cols-2">
          {bills.data.map((b) => (
            <GlassCard key={b.id} hover={false} className="p-5">
              <div className="flex items-center justify-between"><h2 className="text-xl font-bold">{formatMonth(b.month)}</h2><Badge tone={TONE[b.status]}>{b.status.toLowerCase()}</Badge></div>
              <p className="mt-2 font-display text-3xl font-bold text-heading">{formatINR(b.total)}</p>
              <p className="text-sm text-ink-soft">{b.mealCount} meals · due {dayLabel(b.dueDate)}</p>
              <div className="mt-4 flex gap-2">
                <Button variant="ghost" className="!py-2" onClick={() => setDetail(b.id)}>Details</Button>
                {b.status !== 'PAID' && <Button className="!py-2" onClick={() => setPaying(b)}>Pay now</Button>}
              </div>
            </GlassCard>
          ))}
        </div>
      ) : <EmptyState icon={Receipt} title="No bills yet" text="Your first bill appears after the month ends." />}
      <DetailDialog id={detail} onClose={() => setDetail(null)} />
      <PayDialog bill={paying} onClose={() => setPaying(null)} />
    </>
  )
}
