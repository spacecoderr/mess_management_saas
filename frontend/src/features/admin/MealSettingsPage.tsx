import { useState } from 'react'
import { toast } from 'sonner'
import { useSaveSlot, useSlots } from '@/api/ops'
import { MEAL_META } from '@/components/meal/MealCard'
import { Button } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import type { MealSlot } from '@/types/api'

function SlotCard({ slot }: { slot: MealSlot }) {
  const save = useSaveSlot()
  const [s, setS] = useState({ ...slot, startTime: slot.startTime.slice(0, 5), endTime: slot.endTime.slice(0, 5) })
  const m = MEAL_META[slot.mealType]
  const field = (label: string, node: React.ReactNode) => <label className="block text-sm font-semibold text-heading">{label}{node}</label>
  return (
    <GlassCard hover={false} className="space-y-3 p-5">
      <h2 className="flex items-center gap-2 text-xl font-bold"><m.icon size={20} className="text-primary" aria-hidden /> {m.label}</h2>
      <div className="grid grid-cols-2 gap-3">
        {field('Starts', <input type="time" className="field mt-1" value={s.startTime} onChange={(e) => setS({ ...s, startTime: e.target.value })} />)}
        {field('Ends', <input type="time" className="field mt-1" value={s.endTime} onChange={(e) => setS({ ...s, endTime: e.target.value })} />)}
        {field('Price (₹)', <input type="number" min={0} className="field mt-1" value={s.price} onChange={(e) => setS({ ...s, price: Number(e.target.value) })} />)}
        {field('Booking cutoff (minutes before)', <input type="number" min={0} className="field mt-1" value={s.cutoffMinutes} onChange={(e) => setS({ ...s, cutoffMinutes: Number(e.target.value) })} />)}</div>
      <label className="flex items-center gap-2 text-sm font-semibold text-heading"><input type="checkbox" checked={s.active} onChange={(e) => setS({ ...s, active: e.target.checked })} /> Serving this meal</label>
      <Button loading={save.isPending} onClick={() => save.mutateAsync(s).then(() => toast.success(`${m.label} updated`)).catch((e) => toast.error(errorMessage(e)))}>Save</Button>
    </GlassCard>
  )
}

export default function MealSettingsPage() {
  const slots = useSlots()
  return (<><PageHeader title="Meal settings" subtitle="Serving times, prices and booking cutoffs. Prices apply to new bookings only." />
    {slots.isLoading ? <Skeleton className="h-64" /> : <div className="grid gap-4 md:grid-cols-2">{slots.data?.map((s) => <SlotCard key={s.mealType} slot={s} />)}</div>}</>)
}
