import { useMemo, useState } from 'react'
import { ChevronLeft, ChevronRight, Copy } from 'lucide-react'
import { toast } from 'sonner'
import { useCopyWeek } from '@/api/ops'
import { Button } from '@/components/ui/Button'
import { PageHeader } from '@/components/ui/PageHeader'
import { MenuGrid } from '@/features/common/MenuGrid'
import { addDays, dayLabel, isoDate } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'

export default function MenuBuilderPage() {
  const [week, setWeek] = useState(0)
  const start = useMemo(() => addDays(new Date(), week * 7), [week])
  const days = useMemo(() => Array.from({ length: 7 }, (_, i) => isoDate(addDays(start, i))), [start])
  const copy = useCopyWeek()
  return (
    <>
      <PageHeader title="Menu builder" subtitle="Plan the week. Copied menus start as drafts." action={
        <Button variant="ghost" loading={copy.isPending} onClick={() => copy.mutateAsync({ fromWeekStart: days[0]!, toWeekStart: isoDate(addDays(start, 7)) }).then((r) => toast.success(`Copied ${r.copied}, skipped ${r.skipped} to next week`)).catch((e) => toast.error(errorMessage(e)))}><Copy size={16} /> Copy to next week</Button>} />
      <div className="mb-4 flex items-center gap-3"><button type="button" aria-label="Previous week" disabled={week <= 0} className="btn-ghost !p-2.5" onClick={() => setWeek(week - 1)}><ChevronLeft size={18} /></button>
        <span className="font-bold text-heading">{dayLabel(days[0]!)} – {dayLabel(days[6]!)}</span><button type="button" aria-label="Next week" className="btn-ghost !p-2.5" onClick={() => setWeek(week + 1)}><ChevronRight size={18} /></button></div>
      <MenuGrid days={days} />
    </>
  )
}
