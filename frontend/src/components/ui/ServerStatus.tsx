import { useEffect, useState } from 'react'
import { getHealth } from '@/api/health'
import { cn } from '@/lib/cn'

type State = 'checking' | 'up' | 'down'

/** Pings the backend. On Render's free tier the first call can take ~30-50 s while the server wakes. */
export function ServerStatus() {
  const [state, setState] = useState<State>('checking')

  useEffect(() => {
    let cancelled = false
    getHealth()
      .then((h) => !cancelled && setState(h.status === 'UP' ? 'up' : 'down'))
      .catch(() => !cancelled && setState('down'))
    return () => { cancelled = true }
  }, [])

  const label = { checking: 'Waking server…', up: 'API & database online', down: 'API offline' }[state]
  return (
    <span className="glass inline-flex items-center gap-2 !rounded-full px-4 py-1.5 text-xs font-semibold text-forest-800">
      <span className={cn('h-2 w-2 rounded-full', state === 'up' && 'bg-forest-500', state === 'down' && 'bg-terracotta-500', state === 'checking' && 'animate-pulse bg-sage-500')} />
      {label}
    </span>
  )
}
