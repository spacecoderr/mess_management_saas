import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import { CalendarCheck, QrCode, Star } from 'lucide-react'
import { MeshBackground } from '@/components/ui/MeshBackground'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { Logo } from '@/components/ui/Logo'

const POINTS = [
  { icon: CalendarCheck, text: 'Book meals before the cutoff' },
  { icon: QrCode, text: 'Skip the queue with a QR pass' },
  { icon: Star, text: 'Rate what you eat, shape the menu' },
]

/** A rotating thali: plate, katoris and a roti, drawn in SVG. Purely decorative. */
function Thali() {
  return (
    <motion.svg viewBox="0 0 200 200" className="mx-auto h-56 w-56" aria-hidden animate={{ rotate: 360 }} transition={{ duration: 90, repeat: Infinity, ease: 'linear' }}>
      <circle cx="100" cy="100" r="92" fill="rgb(var(--primary))" opacity="0.9" />
      <circle cx="100" cy="100" r="80" fill="none" stroke="rgb(var(--on-primary))" strokeOpacity="0.35" strokeWidth="2" />
      {[
        [100, 44, '#C8643B'], [150, 78, '#D9B15A'], [132, 138, '#8FA58A'], [68, 138, '#E8A084'], [50, 78, '#F3EBDA'],
      ].map(([x, y, c], i) => (
        <g key={i}>
          <circle cx={x as number} cy={y as number} r="22" fill={c as string} />
          <circle cx={x as number} cy={y as number} r="14" fill="#fff" opacity="0.25" />
        </g>
      ))}
      <circle cx="100" cy="100" r="20" fill="#F3EBDA" />
      <circle cx="100" cy="100" r="9" fill="#C8643B" opacity="0.8" />
    </motion.svg>
  )
}

export function AuthLayout({ title, subtitle, children, footer }: { title: string; subtitle?: string; children: ReactNode; footer?: ReactNode }) {
  return (
    <>
      <MeshBackground />
      <div className="mx-auto grid min-h-screen max-w-6xl items-center gap-10 px-4 py-8 lg:grid-cols-[1fr_30rem]">
        <section className="hidden lg:block">
          <Logo className="mb-10 text-2xl" />
          <h2 className="max-w-md text-5xl font-bold leading-tight">
            Your mess, <span className="text-accent">made effortless.</span>
          </h2>
          <p className="mt-4 max-w-md text-lg text-ink-soft">The mess system for students and staff of IIIT Bhubaneswar.</p>
          <ul className="mt-8 space-y-3">
            {POINTS.map(({ icon: Icon, text }) => (
              <li key={text} className="flex items-center gap-3 font-semibold text-heading">
                <span className="grid h-9 w-9 place-items-center rounded-xl bg-primary/10 text-primary">
                  <Icon size={18} aria-hidden />
                </span>
                {text}
              </li>
            ))}
          </ul>
          <div className="mt-10">
            <Thali />
          </div>
        </section>

        <main>
          <div className="mb-4 flex items-center justify-between lg:hidden">
            <Logo />
            <ThemeToggle />
          </div>
          <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.45 }} className="glass-strong p-6 sm:p-8">
            <div className="mb-6 flex items-start justify-between gap-3">
              <div>
                <h1 className="text-3xl font-bold">{title}</h1>
                {subtitle && <p className="mt-1 text-ink-soft">{subtitle}</p>}
              </div>
              <ThemeToggle className="hidden lg:grid" />
            </div>
            {children}
          </motion.div>
          {footer && <p className="mt-5 text-center text-sm text-ink-soft">{footer}</p>}
        </main>
      </div>
    </>
  )
}
