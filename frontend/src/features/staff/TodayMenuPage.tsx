import { MenuGrid } from '@/features/common/MenuGrid'
import { PageHeader } from '@/components/ui/PageHeader'
import { addDays, isoDate } from '@/lib/dates'

export default function TodayMenuPage() {
  const now = new Date()
  return (<><PageHeader title="Today's menu" subtitle="Set dishes for today and tomorrow, then publish so students can book." /><MenuGrid days={[isoDate(now), isoDate(addDays(now, 1)), isoDate(addDays(now, 2))]} /></>)
}
