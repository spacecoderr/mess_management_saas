import { useState } from 'react'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'
import { useDishes, useRetireDish, useSaveDish } from '@/api/ops'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'

const CATS = ['MAIN', 'SIDE', 'BREAD', 'RICE', 'DESSERT', 'BEVERAGE']
const blank: { name: string; category: string; veg: boolean } = { name: '', category: 'MAIN', veg: true }

export default function DishesPage() {
  const dishes = useDishes(), save = useSaveDish(), retire = useRetireDish()
  const [edit, setEdit] = useState<{ id?: number; name: string; category: string; veg: boolean } | null>(null)
  const [q, setQ] = useState('')
  const shown = (dishes.data ?? []).filter((d) => d.name.toLowerCase().includes(q.toLowerCase()))
  return (
    <>
      <PageHeader title="Dishes" subtitle="The library menus are built from." action={<Button onClick={() => setEdit({ ...blank })}><Plus size={16} /> New dish</Button>} />
      <input aria-label="Search dishes" className="field mb-4 !w-64" placeholder="Search dishes" value={q} onChange={(e) => setQ(e.target.value)} />
      {dishes.isLoading ? <Skeleton className="h-48" /> : <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">{shown.map((d) => (
        <li key={d.id} className="glass flex items-center justify-between gap-2 !rounded-2xl px-4 py-3"><div><p className="font-bold text-heading">{d.name} {!d.active && <Badge tone="danger">retired</Badge>}</p><p className="text-xs text-ink-soft">{d.category.toLowerCase()} · {d.veg ? 'veg' : 'non-veg'}</p></div>
          <div className="flex gap-1"><Button variant="ghost" className="!px-3 !py-1.5" onClick={() => setEdit({ id: d.id, name: d.name, category: d.category, veg: d.veg })}>Edit</Button>{d.active && <Button variant="ghost" className="!px-3 !py-1.5" onClick={() => retire.mutateAsync(d.id).then(() => toast.success('Dish retired'))}>Retire</Button>}</div></li>))}</ul>}
      {edit && <Modal open onClose={() => setEdit(null)} title={edit.id ? 'Edit dish' : 'New dish'}>
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutateAsync({ id: edit.id, name: edit.name, category: edit.category, veg: edit.veg, active: true }).then(() => { toast.success('Dish saved'); setEdit(null) }) }}>
          <Input label="Name" value={edit.name} onChange={(e) => setEdit({ ...edit, name: e.target.value })} />
          <label className="block text-sm font-semibold text-heading">Category<select className="field mt-1" value={edit.category} onChange={(e) => setEdit({ ...edit, category: e.target.value })}>{CATS.map((c) => <option key={c} value={c}>{c.toLowerCase()}</option>)}</select></label>
          <label className="flex items-center gap-2 text-sm font-semibold text-heading"><input type="checkbox" checked={edit.veg} onChange={(e) => setEdit({ ...edit, veg: e.target.checked })} /> Vegetarian</label>
          {save.error && <Alert tone="error">{errorMessage(save.error)}</Alert>}
          <Button type="submit" className="w-full" disabled={!edit.name.trim()} loading={save.isPending}>Save</Button></form></Modal>}
    </>
  )
}
