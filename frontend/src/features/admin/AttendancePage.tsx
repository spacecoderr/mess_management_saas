import { useState } from 'react'
import { Download } from 'lucide-react'
import { toast } from 'sonner'
import { downloadCsv } from '@/api/ops'
import { useLiveCounts } from '@/api/queries'
import { MEAL_META } from '@/components/meal/MealCard'
import { Button } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { addDays, isoDate } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'

export default function AttendancePage() {
  const live = useLiveCounts()
  const [from, setFrom] = useState(isoDate(addDays(new Date(), -6))), [to, setTo] = useState(isoDate(new Date()))
  return (
    <>
      <PageHeader title="Attendance" subtitle="Today's counts and CSV exports." />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{live.data?.map((m) => <GlassCard key={m.mealType} hover={false} className="p-5"><p className="font-bold text-heading">{MEAL_META[m.mealType].label}</p><p className="mt-2 font-display text-3xl font-bold text-heading">{m.served}<span className="text-lg text-ink-soft"> / {m.booked}</span></p><p className="text-xs text-ink-soft">{m.remaining} waiting · {m.noShow} no-show</p></GlassCard>)}</div>
      <GlassCard hover={false} className="mt-6 flex flex-wrap items-end gap-3 p-5">
        <label className="text-sm font-semibold text-heading">From<input type="date" className="field mt-1" value={from} onChange={(e) => setFrom(e.target.value)} /></label>
        <label className="text-sm font-semibold text-heading">To<input type="date" className="field mt-1" value={to} onChange={(e) => setTo(e.target.value)} /></label>
        <Button onClick={() => downloadCsv(`/reports/attendance.csv?from=${from}&to=${to}`, `attendance-${from}_${to}.csv`).catch((e) => toast.error(errorMessage(e)))}><Download size={16} /> Download attendance CSV</Button></GlassCard>
    </>
  )
}
