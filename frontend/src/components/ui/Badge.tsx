import type { ReactNode } from 'react'
import { cn } from '@/lib/cn'

const TONE = {
  neutral: 'bg-ink/10 text-ink',
  success: 'bg-success/15 text-success',
  danger: 'bg-danger/15 text-danger',
  accent: 'bg-accent/15 text-accent',
  primary: 'bg-primary/15 text-primary',
} as const

export function Badge({ tone = 'neutral', children, className }: { tone?: keyof typeof TONE; children: ReactNode; className?: string }) {
  return <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-xs font-bold', TONE[tone], className)}>{children}</span>
}
