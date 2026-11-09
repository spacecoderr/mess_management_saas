import { Star } from 'lucide-react'
import { cn } from '@/lib/cn'

const WORDS = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent']

/** Interactive 1-5 stars (radio group, keyboard friendly) or a read-only display. */
export function StarRating({ value, onChange, label = 'Rating' }: { value: number; onChange?: (n: number) => void; label?: string }) {
  return (
    <div role={onChange ? 'radiogroup' : 'img'} aria-label={onChange ? label : `${value} out of 5 stars`} className="flex items-center gap-1">
      {[1, 2, 3, 4, 5].map((n) =>
        onChange ? (
          <button key={n} type="button" role="radio" aria-checked={value === n} aria-label={`${n} star${n > 1 ? 's' : ''}`} onClick={() => onChange(n)} className="rounded-full p-0.5 transition hover:scale-110">
            <Star size={26} className={cn(n <= value ? 'fill-accent text-accent' : 'text-ink-soft/50')} />
          </button>
        ) : (
          <Star key={n} size={16} className={cn(n <= value ? 'fill-accent text-accent' : 'text-ink-soft/40')} />
        ),
      )}
      {onChange && value > 0 && <span className="ml-2 text-sm font-semibold text-ink-soft">{WORDS[value]}</span>}
    </div>
  )
}
