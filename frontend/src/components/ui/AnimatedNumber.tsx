import { useEffect, useState } from 'react'
import { animate, useReducedMotion } from 'framer-motion'

/** Counts up to `value` once on mount and whenever it changes (instantly if the user prefers reduced motion). */
export function AnimatedNumber({ value, format = (n: number) => Math.round(n).toLocaleString('en-IN') }: { value: number; format?: (n: number) => string }) {
  const reduce = useReducedMotion()
  const [shown, setShown] = useState(reduce ? value : 0)
  useEffect(() => {
    if (reduce) {
      setShown(value)
      return
    }
    const controls = animate(shown, value, { duration: 0.9, ease: 'easeOut', onUpdate: setShown })
    return () => controls.stop()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value, reduce])
  return <span>{format(shown)}</span>
}
