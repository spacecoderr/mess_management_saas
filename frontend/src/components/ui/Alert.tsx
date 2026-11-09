import type { ReactNode } from 'react'
import { AlertCircle, CheckCircle2, Info } from 'lucide-react'
import { cn } from '@/lib/cn'

const STYLE = {
  error: { box: 'border-danger/40 text-danger', icon: AlertCircle },
  success: { box: 'border-success/40 text-success', icon: CheckCircle2 },
  info: { box: 'border-primary/30 text-heading', icon: Info },
} as const

export function Alert({ tone = 'info', children, className }: { tone?: keyof typeof STYLE; children: ReactNode; className?: string }) {
  const { box, icon: Icon } = STYLE[tone]
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cn('flex items-start gap-3 rounded-2xl border px-4 py-3 text-sm', box, className)} style={{ background: 'var(--glass-bg)' }}>
      <Icon className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <div>{children}</div>
    </div>
  )
}
