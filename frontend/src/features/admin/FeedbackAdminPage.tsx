import { useDishScores, useFeedbackList, useFeedbackSummary } from '@/api/ops'
import { MEAL_META } from '@/components/meal/MealCard'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { StarRating } from '@/components/ui/StarRating'
import { dayLabel } from '@/lib/dates'

export default function FeedbackAdminPage() {
  const sum = useFeedbackSummary(), list = useFeedbackList(), best = useDishScores('best'), worst = useDishScores('worst')
  const total = sum.data?.totalRatings ?? 0
  const dishList = (title: string, d?: { dishId: number; name: string; averageRating: number; ratings: number }[]) => (
    <GlassCard hover={false} className="p-5"><h2 className="mb-3 text-lg font-bold">{title}</h2><ol className="space-y-1 text-sm">{d?.map((x) => <li key={x.dishId} className="flex justify-between"><span className="text-heading">{x.name}</span><span className="text-ink-soft">{x.averageRating.toFixed(2)} ★ ({x.ratings})</span></li>)}</ol></GlassCard>)
  return (
    <>
      <PageHeader title="Feedback" subtitle="What students think of the food (last 30 days)." />
      {sum.isLoading ? <Skeleton className="h-40" /> : (
        <div className="grid gap-4 lg:grid-cols-3">
          <GlassCard hover={false} className="p-5"><p className="text-sm font-semibold text-ink-soft">Average rating</p><p className="font-display text-4xl font-bold text-heading">{sum.data?.averageRating?.toFixed(2) ?? '—'}</p><p className="text-xs text-ink-soft">{total} ratings</p>
            <div className="mt-3 space-y-1">{[5, 4, 3, 2, 1].map((n) => <div key={n} className="flex items-center gap-2 text-xs"><span className="w-3">{n}</span><div className="h-2 flex-1 rounded-full bg-ink/10"><div className="h-2 rounded-full bg-accent" style={{ width: `${total ? ((sum.data?.distribution[n] ?? 0) / total) * 100 : 0}%` }} /></div></div>)}</div></GlassCard>
          {dishList('Best rated dishes', best.data)}{dishList('Needs attention', worst.data)}</div>)}
      <h2 className="mb-3 mt-8 text-xl font-bold">Recent ratings</h2>
      <ul className="space-y-2">{list.data?.content.map((f) => <li key={f.id} className="glass flex flex-wrap items-center justify-between gap-2 !rounded-2xl px-4 py-3"><span className="text-sm font-semibold text-heading">{MEAL_META[f.mealType].label} · {dayLabel(f.date)} <span className="font-normal text-ink-soft">· {f.studentName}</span></span><StarRating value={f.rating} />{f.comment && <p className="w-full text-sm text-ink-soft">“{f.comment}”</p>}</li>)}</ul>
    </>
  )
}
