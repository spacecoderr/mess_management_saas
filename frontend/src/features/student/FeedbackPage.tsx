import { useState } from 'react'
import { MessageSquareHeart } from 'lucide-react'
import { toast } from 'sonner'
import { useMyFeedback, usePendingFeedback, useRate } from '@/api/student'
import { MEAL_META } from '@/components/meal/MealCard'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { StarRating } from '@/components/ui/StarRating'
import { dayLabel } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import type { PendingFeedback } from '@/types/api'

function RateCard({ p }: { p: PendingFeedback }) {
  const [stars, setStars] = useState(0)
  const [comment, setComment] = useState('')
  const rate = useRate()
  const meta = MEAL_META[p.mealType]
  return (
    <GlassCard hover={false} className="space-y-3 p-5">
      <div><h3 className="text-lg font-bold">{meta.label} · {dayLabel(p.date)}</h3><p className="text-sm text-ink-soft">{p.dishes.join(', ')}</p></div>
      <StarRating value={stars} onChange={setStars} label={`Rate ${meta.label}`} />
      <label className="block text-sm font-semibold text-heading">Comment (optional)
        <textarea className="field mt-1" rows={2} maxLength={1000} value={comment} onChange={(e) => setComment(e.target.value)} /></label>
      <Button disabled={stars === 0} loading={rate.isPending}
        onClick={() => rate.mutateAsync({ dailyMenuId: p.dailyMenuId, rating: stars, comment: comment || undefined }).then(() => toast.success('Thanks for your feedback!')).catch((e) => toast.error(errorMessage(e)))}>Submit rating</Button>
    </GlassCard>
  )
}

export default function FeedbackPage() {
  const pending = usePendingFeedback(), mine = useMyFeedback()
  return (
    <>
      <PageHeader title="Feedback" subtitle="Rate the meals you've eaten. It helps the mess improve." />
      <h2 className="mb-3 text-xl font-bold">To rate</h2>
      {pending.isLoading ? <Skeleton className="h-40" /> : pending.data?.length
        ? <div className="grid gap-4 md:grid-cols-2">{pending.data.map((p) => <RateCard key={p.dailyMenuId} p={p} />)}</div>
        : <EmptyState icon={MessageSquareHeart} title="All caught up" text="Meals you eat will show up here for rating." />}
      {!!mine.data?.length && (
        <section className="mt-8"><h2 className="mb-3 text-xl font-bold">Your ratings</h2>
          <ul className="space-y-2">{mine.data.map((f) => (
            <li key={f.id} className="glass flex flex-wrap items-center justify-between gap-2 !rounded-2xl px-4 py-3">
              <span className="font-semibold text-heading">{MEAL_META[f.mealType].label} · {dayLabel(f.date)}</span>
              <StarRating value={f.rating} />{f.comment && <p className="w-full text-sm text-ink-soft">“{f.comment}”</p>}
            </li>))}</ul></section>
      )}
    </>
  )
}
