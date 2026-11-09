import { lazy, Suspense, useState } from 'react'
import { CheckCircle2, Search, XCircle } from 'lucide-react'
import { useManual, useScan, useStaffBookings } from '@/api/ops'
import { MEAL_META } from '@/components/meal/MealCard'
import { Button } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { Tabs } from '@/components/ui/Tabs'
import { isoDate } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import type { MealType, ScanResult } from '@/types/api'

const CameraScanner = lazy(() => import('./CameraScanner'))
const guessMeal = (): MealType => { const h = new Date().getHours(); return h < 10 ? 'BREAKFAST' : h < 15 ? 'LUNCH' : h < 19 ? 'SNACKS' : 'DINNER' }
const token = (raw: string) => raw.trim().replace(/^THALI:/i, '')

export function ScanPanel() {
  const [meal, setMeal] = useState<MealType>(guessMeal)
  const [code, setCode] = useState('')
  const [q, setQ] = useState('')
  const [result, setResult] = useState<{ ok: true; r: ScanResult } | { ok: false; msg: string } | null>(null)
  const scan = useScan(), manual = useManual()
  const list = useStaffBookings(isoDate(new Date()), meal, q)

  const submit = (raw: string) => scan.mutateAsync(token(raw)).then((r) => setResult({ ok: true, r })).catch((e) => setResult({ ok: false, msg: errorMessage(e) })).finally(() => setCode(''))
  const mark = (id: number) => manual.mutateAsync(id).then((r) => setResult({ ok: true, r })).catch((e) => setResult({ ok: false, msg: errorMessage(e) }))

  return (
    <GlassCard hover={false} className="mt-6 space-y-5 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-bold">Check students in</h2>
        <Tabs label="Meal" value={meal} onChange={setMeal} options={(Object.keys(MEAL_META) as MealType[]).map((m) => ({ value: m, label: MEAL_META[m].label }))} /></div>
      <div className="grid gap-6 lg:grid-cols-2">
        <div className="space-y-4">
          <Suspense fallback={null}><CameraScanner onScan={submit} /></Suspense>
          <form onSubmit={(e) => { e.preventDefault(); if (code.trim()) void submit(code) }} className="flex gap-2">
            <input aria-label="Pass code" className="field" placeholder="Scan or paste pass code" value={code} onChange={(e) => setCode(e.target.value)} /><Button type="submit" loading={scan.isPending}>Check in</Button></form>
          <div aria-live="polite">{result && (result.ok
            ? <div className="flex items-center gap-3 rounded-2xl bg-success/15 p-4 text-success"><CheckCircle2 aria-hidden /><div><p className="font-bold">{result.r.studentName} checked in</p><p className="text-sm">{result.r.rollNo} · {MEAL_META[result.r.mealType].label} · {result.r.dietPref?.replace('_', '-').toLowerCase()}</p></div></div>
            : <div role="alert" className="flex items-center gap-3 rounded-2xl bg-danger/15 p-4 text-danger"><XCircle aria-hidden /><p className="font-bold">{result.msg}</p></div>)}</div>
        </div>
        <div>
          <div className="relative mb-3"><Search size={16} className="absolute left-4 top-1/2 -translate-y-1/2 text-ink-soft" aria-hidden /><input aria-label="Search by name or roll number" className="field pl-11" placeholder="Name or roll number" value={q} onChange={(e) => setQ(e.target.value)} /></div>
          <ul className="max-h-72 space-y-2 overflow-y-auto" aria-label="Waiting students">
            {list.data?.length === 0 && <li className="text-sm text-ink-soft">Nobody is waiting for this meal.</li>}
            {list.data?.map((b) => <li key={b.id} className="glass flex items-center justify-between !rounded-2xl px-4 py-2"><span className="text-sm"><strong className="text-heading">{b.studentName}</strong> <span className="text-ink-soft">{b.rollNo}</span></span><Button variant="ghost" className="!px-3 !py-1.5" loading={manual.isPending && manual.variables === b.id} onClick={() => mark(b.id)}>Mark served</Button></li>)}
          </ul>
        </div>
      </div>
    </GlassCard>
  )
}
