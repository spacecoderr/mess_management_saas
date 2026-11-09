import { useId } from 'react'
import { motion } from 'framer-motion'
import { cn } from '@/lib/cn'

interface Props<T extends string> {
  value: T
  onChange: (v: T) => void
  options: { value: T; label: string }[]
  label: string
}

/** Segmented control with an animated pill. Arrow keys move between options (role=tablist pattern). */
export function Tabs<T extends string>({ value, onChange, options, label }: Props<T>) {
  const id = useId()
  return (
    <div
      role="tablist"
      aria-label={label}
      className="relative flex rounded-full border p-1"
      style={{ background: 'var(--glass-bg)', borderColor: 'var(--glass-border)' }}
      onKeyDown={(e) => {
        const i = options.findIndex((o) => o.value === value)
        if (e.key === 'ArrowRight') onChange(options[(i + 1) % options.length]!.value)
        if (e.key === 'ArrowLeft') onChange(options[(i - 1 + options.length) % options.length]!.value)
      }}
    >
      {options.map((o) => {
        const active = o.value === value
        return (
          <button
            key={o.value}
            role="tab"
            type="button"
            aria-selected={active}
            tabIndex={active ? 0 : -1}
            onClick={() => onChange(o.value)}
            className={cn('relative z-10 flex-1 rounded-full px-4 py-2 text-sm font-semibold transition-colors', active ? 'text-on-primary' : 'text-ink-soft hover:text-heading')}
          >
            {active && <motion.span layoutId={`tab-pill-${id}`} className="absolute inset-0 -z-10 rounded-full bg-primary" transition={{ type: 'spring', stiffness: 400, damping: 32 }} />}
            {o.label}
          </button>
        )
      })}
    </div>
  )
}
