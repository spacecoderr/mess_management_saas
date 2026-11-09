import { useState } from 'react'
import { Download, Receipt } from 'lucide-react'
import { toast } from 'sonner'
import { downloadCsv, useAdminBills, useCash, useGenerate } from '@/api/ops'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import { formatINR } from '@/lib/format'

const TONE = { PAID: 'success', UNPAID: 'accent', OVERDUE: 'danger' } as const
const lastMonth = () => { const d = new Date(); d.setMonth(d.getMonth() - 1); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}` }

export default function BillingPage() {
  const [month, setMonth] = useState(lastMonth()), [status, setStatus] = useState('')
  const bills = useAdminBills(month, status), gen = useGenerate(), cash = useCash()
  const generate = () => gen.mutateAsync(month).then((r) => toast.success(`${r.created} created, ${r.updated} updated, ${r.skippedPaid} already paid`)).catch((e) => toast.error(errorMessage(e)))
  return (
    <>
      <PageHeader title="Billing" subtitle="Generate monthly bills and record cash payments." />
      <div className="mb-4 flex flex-wrap items-end gap-3">
        <label className="text-sm font-semibold text-heading">Month<input type="month" className="field mt-1" value={month} onChange={(e) => setMonth(e.target.value)} /></label>
        <label className="text-sm font-semibold text-heading">Status<select className="field mt-1" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All</option><option value="UNPAID">Unpaid</option><option value="OVERDUE">Overdue</option><option value="PAID">Paid</option></select></label>
        <Button onClick={generate} loading={gen.isPending}><Receipt size={16} /> Generate bills</Button>
        <Button variant="ghost" onClick={() => downloadCsv(`/reports/bills.csv?month=${month}`, `bills-${month}.csv`).catch((e) => toast.error(errorMessage(e)))}><Download size={16} /> Export CSV</Button></div>
      {bills.isLoading ? <Skeleton className="h-48" /> : bills.data?.content.length ? (
        <div className="glass overflow-x-auto !rounded-2xl"><table className="w-full min-w-[40rem] text-left text-sm"><thead className="text-ink-soft"><tr><th className="p-3">Student</th><th>Meals</th><th>Total</th><th>Status</th><th /></tr></thead>
          <tbody>{bills.data.content.map((b) => (<tr key={b.id} className="border-t" style={{ borderColor: 'var(--glass-border)' }}><td className="p-3"><p className="font-bold text-heading">{b.studentName}</p><p className="text-xs text-ink-soft">{b.rollNo}</p></td><td>{b.mealCount}</td><td className="font-semibold">{formatINR(b.total, true)}</td><td><Badge tone={TONE[b.status]}>{b.status.toLowerCase()}</Badge></td>
            <td className="p-3 text-right">{b.status !== 'PAID' && <Button variant="ghost" className="!px-3 !py-1.5" loading={cash.isPending && cash.variables?.billId === b.id} onClick={() => cash.mutateAsync({ billId: b.id, amount: b.total }).then(() => toast.success('Cash payment recorded')).catch((e) => toast.error(errorMessage(e)))}>Record cash</Button>}</td></tr>))}</tbody></table></div>
      ) : <EmptyState icon={Receipt} title="No bills for this month" text="Press Generate bills to create them from the month's meals." />}
    </>
  )
}
