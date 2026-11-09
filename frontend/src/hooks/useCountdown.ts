import { useCallback, useEffect, useState } from 'react'

/** Counts down from `seconds` once started; used for the "resend code in 42s" button. */
export function useCountdown(seconds: number) {
  const [left, setLeft] = useState(0)
  useEffect(() => {
    if (left <= 0) return
    const t = setTimeout(() => setLeft((l) => l - 1), 1000)
    return () => clearTimeout(t)
  }, [left])
  const start = useCallback(() => setLeft(seconds), [seconds])
  return { left, running: left > 0, start }
}
