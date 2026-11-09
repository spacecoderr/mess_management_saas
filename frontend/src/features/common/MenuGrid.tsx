import { useMemo, useState } from 'react'
import { Pencil } from 'lucide-react'
import { toast } from 'sonner'
import { useDishes, usePublish, useSetMenu, useWeekMenus } from '@/api/ops'
import { MEAL_META } from '@/components/meal/MealCard'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Modal } from '@/components/ui/Modal'
import { Skeleton } from '@/components/ui/Skeleton'
import { dayLabel } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import type { DailyMenu, Dish, MealType } from '@/types/api'

const MEALS = Object.keys(MEAL_META) as MealType[]

function Editor({ date, meal, menu, onClose }: { date: string; meal: MealType; menu?: DailyMenu; onClose: () => void }) {
  const dishes = useDishes(), save = useSetMenu(), publish = usePublish()
  const [picked, setPicked] = useState<Set<number>>(new Set(menu?.items.map((i) => i.id)))
  const [filter, setFilter] = useState('')
  const toggle = (id: number) => setPicked((s) => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n })
  const shown = (dishes.data ?? []).filter((d: Dish) => d.active && d.name.toLowerCase().includes(filter.toLowerCase()))
  const go = (pub?: boolean) => save.mutateAsync({ date, mealType: meal, itemIds: [...picked], publish: pub }).then(() => { toast.success(pub ? 'Menu published' : 'Menu saved'); onClose() }).catch((e) => toast.error(errorMessage(e)))
  return (
    <Modal open onClose={onClose} title={`${MEAL_META[meal].label} · ${dayLabel(date)}`}>
      <div className="space-y-3">
        <input aria-label="Filter dishes" className="field" placeholder="Filter dishes" value={filter} onChange={(e) => setFilter(e.target.value)} />
        <ul className="max-h-64 space-y-1 overflow-y-auto" aria-label="Dishes">
          {shown.map((d) => <li key={d.id}><label className="flex cursor-pointer items-center gap-2 rounded-xl px-2 py-1.5 hover:bg-primary/10"><input type="checkbox" checked={picked.has(d.id)} onChange={() => toggle(d.id)} /><span className="text-sm text-heading">{d.name}</span><span className="ml-auto text-xs text-ink-soft">{d.category.toLowerCase()}{d.veg ? '' : ' · non-veg'}</span></label></li>)}
        </ul>
        <p className="text-sm text-ink-soft">{picked.size} selected</p>
        <div className="flex flex-wrap gap-2">
          <Button variant="ghost" onClick={() => go(menu?.published ? true : undefined)} loading={save.isPending}>Save</Button>
          {!menu?.published && <Button onClick={() => go(true)} disabled={picked.size === 0} loading={save.isPending}>Save & publish</Button>}
          {menu?.published && <Button variant="danger" onClick={() => publish.mutateAsync({ id: menu.id, published: false }).then(() => { toast.success('Unpublished'); onClose() }).catch((e) => toast.error(errorMessage(e)))}>Unpublish</Button>}
        </div>
      </div>
    </Modal>
  )
}

/** Days x meals table. Click a cell to edit its dishes and publish. Shared by staff (2 days) and admin (a week). */
export function MenuGrid({ days }: { days: string[] }) {
  const menus = useWeekMenus(days[0]!, days[days.length - 1]!)
  const [edit, setEdit] = useState<{ date: string; meal: MealType } | null>(null)
  const byKey = useMemo(() => new Map((menus.data ?? []).map((m) => [`${m.date}|${m.mealType}`, m])), [menus.data])
  if (menus.isLoading) return <Skeleton className="h-64" />
  return (
    <>
      <div className="overflow-x-auto"><table className="w-full min-w-[40rem] border-separate border-spacing-2 text-left text-sm">
        <thead><tr><th className="p-2 text-ink-soft">Day</th>{MEALS.map((m) => <th key={m} className="p-2 text-heading">{MEAL_META[m].label}</th>)}</tr></thead>
        <tbody>{days.map((d) => (
          <tr key={d}><th scope="row" className="whitespace-nowrap p-2 text-heading">{dayLabel(d)}</th>
            {MEALS.map((m) => { const menu = byKey.get(`${d}|${m}`); return (
              <td key={m} className="glass !rounded-2xl p-0 align-top"><button type="button" aria-label={`Edit ${MEAL_META[m].label} on ${dayLabel(d)}`} onClick={() => setEdit({ date: d, meal: m })} className="flex h-full w-full flex-col gap-1 rounded-2xl p-3 text-left hover:bg-primary/10">
                {menu ? <><Badge tone={menu.published ? 'success' : 'accent'}>{menu.published ? 'Published' : 'Draft'}</Badge><span className="text-xs text-ink-soft">{menu.items.map((i) => i.name).join(', ') || 'No dishes'}</span></> : <span className="flex items-center gap-1 text-xs text-ink-soft"><Pencil size={12} aria-hidden /> Add menu</span>}</button></td>) })}</tr>))}</tbody></table></div>
      {edit && <Editor date={edit.date} meal={edit.meal} menu={byKey.get(`${edit.date}|${edit.meal}`)} onClose={() => setEdit(null)} />}
    </>
  )
}
