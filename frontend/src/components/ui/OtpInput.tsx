import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react'
import { cn } from '@/lib/cn'

interface Props {
  value: string
  onChange: (value: string) => void
  onComplete?: (value: string) => void
  length?: number
  disabled?: boolean
  invalid?: boolean
  label?: string
}

/** Six boxes, one digit each: auto-advance, backspace goes back, arrows move, pasting a code fills everything. */
export function OtpInput({ value, onChange, onComplete, length = 6, disabled, invalid, label = 'One-time code' }: Props) {
  const refs = useRef<(HTMLInputElement | null)[]>([])
  const digits = Array.from({ length }, (_, i) => value[i] ?? '')

  const commit = (next: string) => {
    const clean = next.replace(/\D/g, '').slice(0, length)
    onChange(clean)
    if (clean.length === length) onComplete?.(clean)
  }

  const focus = (i: number) => refs.current[Math.max(0, Math.min(length - 1, i))]?.focus()

  const onKeyDown = (i: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace') {
      e.preventDefault()
      if (digits[i]) commit(value.slice(0, i) + value.slice(i + 1))
      else if (i > 0) {
        commit(value.slice(0, i - 1) + value.slice(i))
        focus(i - 1)
      }
    } else if (e.key === 'ArrowLeft') focus(i - 1)
    else if (e.key === 'ArrowRight') focus(i + 1)
  }

  const onPaste = (e: ClipboardEvent<HTMLInputElement>) => {
    e.preventDefault()
    const text = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, length)
    if (!text) return
    commit(text)
    focus(text.length >= length ? length - 1 : text.length)
  }

  return (
    <div role="group" aria-label={label} className="flex justify-between gap-2">
      {digits.map((d, i) => (
        <input
          key={i}
          ref={(el) => {
            refs.current[i] = el
          }}
          value={d}
          inputMode="numeric"
          autoComplete={i === 0 ? 'one-time-code' : 'off'}
          disabled={disabled}
          aria-label={`Digit ${i + 1} of ${length}`}
          aria-invalid={invalid || undefined}
          onFocus={(e) => e.target.select()}
          onKeyDown={(e) => onKeyDown(i, e)}
          onPaste={onPaste}
          onChange={(e) => {
            // No maxLength on purpose: OTP autofill (iOS/Android) drops the WHOLE code into one box, and a maxLength would truncate it.
            const typed = e.target.value.replace(/\D/g, '')
            if (!typed) return
            if (typed.length > 1) {
              // a box that already held a digit then received one more: keep just the new one; otherwise spread the code from this box on
              const spread = d && typed.length === 2 && typed.startsWith(d) ? typed.slice(1) : typed
              commit(value.slice(0, i) + spread)
              return focus(Math.min(length - 1, i + spread.length))
            }
            commit(value.slice(0, i) + typed + value.slice(i + 1))
            if (i < length - 1) focus(i + 1)
          }}
          className={cn('field !px-0 h-14 w-full min-w-0 text-center font-display text-2xl font-bold', d && 'border-primary')}
        />
      ))}
    </div>
  )
}
