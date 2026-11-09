import { useEffect } from 'react'
import { Moon, Sun } from 'lucide-react'
import { applyTheme, resolveTheme, useThemeStore } from '@/store/themeStore'

/** Applies the stored theme (and follows the OS in "system" mode) and renders a sun/moon button. */
export function useThemeSync() {
  const mode = useThemeStore((s) => s.mode)
  useEffect(() => {
    applyTheme(mode)
    if (mode !== 'system' || !window.matchMedia) return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = () => applyTheme('system')
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [mode])
}

export function ThemeToggle({ className }: { className?: string }) {
  const mode = useThemeStore((s) => s.mode)
  const setMode = useThemeStore((s) => s.setMode)
  const dark = resolveTheme(mode) === 'dark'
  return (
    <button
      type="button"
      onClick={() => setMode(dark ? 'light' : 'dark')}
      aria-label={dark ? 'Switch to light theme' : 'Switch to dark theme'}
      className={`grid h-10 w-10 place-items-center rounded-full border text-heading transition hover:scale-105 ${className ?? ''}`}
      style={{ background: 'var(--glass-bg)', borderColor: 'var(--glass-border)' }}
    >
      {dark ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  )
}
