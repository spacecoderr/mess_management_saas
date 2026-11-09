import { Check } from 'lucide-react'
import { cn } from '@/lib/cn'

export function StepIndicator({ steps, current }: { steps: string[]; current: number }) {
  return (
    <ol className="flex items-center gap-2" aria-label="Progress">
      {steps.map((label, i) => {
        const done = i < current
        const active = i === current
        return (
          <li key={label} className="flex flex-1 items-center gap-2" aria-current={active ? 'step' : undefined}>
            <span
              className={cn(
                'grid h-8 w-8 shrink-0 place-items-center rounded-full text-sm font-bold transition-colors',
                done || active ? 'bg-primary text-on-primary' : 'text-ink-soft',
              )}
              style={done || active ? undefined : { background: 'var(--glass-bg)' }}
            >
              {done ? <Check size={16} aria-label="done" /> : i + 1}
            </span>
            <span className={cn('hidden text-sm font-semibold sm:block', active ? 'text-heading' : 'text-ink-soft')}>{label}</span>
            {i < steps.length - 1 && <span className={cn('h-0.5 flex-1 rounded', done ? 'bg-primary' : 'bg-ink/15')} />}
          </li>
        )
      })}
    </ol>
  )
}
