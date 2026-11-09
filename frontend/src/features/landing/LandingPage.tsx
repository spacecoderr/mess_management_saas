import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { ArrowRight, BarChart3, CalendarCheck, CreditCard, MessageSquareWarning, QrCode, ShieldCheck, Star, UtensilsCrossed, Users } from 'lucide-react'
import { MEAL_META } from '@/components/meal/MealCard'
import { buttonClass } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { Logo } from '@/components/ui/Logo'
import { MeshBackground } from '@/components/ui/MeshBackground'
import { ServerStatus } from '@/components/ui/ServerStatus'
import { ThemeToggle } from '@/components/ui/ThemeToggle'
import { ROLE_HOME, useAuthStore } from '@/store/authStore'
import type { MealType } from '@/types/api'

const SAMPLE: { type: MealType; time: string; items: string }[] = [
  { type: 'BREAKFAST', time: '7:30 – 9:00', items: 'Idli, sambar, chutney' },
  { type: 'LUNCH', time: '12:30 – 2:30', items: 'Rice, dal, paneer curry' },
  { type: 'SNACKS', time: '5:00 – 6:00', items: 'Samosa, masala chai' },
  { type: 'DINNER', time: '8:00 – 9:30', items: 'Roti, mixed veg, kheer' },
]

const FEATURES = [
  { icon: CalendarCheck, title: 'Daily meal booking', text: 'Book or cancel meals before the cutoff, or the whole week in one tap.' },
  { icon: QrCode, title: 'QR attendance', text: 'A single-use QR pass per booking. Staff scan it at the counter.' },
  { icon: UtensilsCrossed, title: 'Menu management', text: 'Plan weekly menus, copy last week, and publish when ready.' },
  { icon: Star, title: 'Ratings & feedback', text: 'Rate the meals you actually ate and help the mess improve.' },
  { icon: MessageSquareWarning, title: 'Complaints', text: 'Raise an issue and follow it from open to resolved.' },
  { icon: CreditCard, title: 'Monthly billing', text: 'Transparent bills, instant payment and receipts.' },
  { icon: BarChart3, title: 'Analytics', text: 'Footfall, wastage, ratings and revenue at a glance.' },
  { icon: ShieldCheck, title: 'College-verified', text: 'Sign in with your @iiit-bh.ac.in email and a one-time code.' },
]

const ROLES = [
  { icon: Users, title: 'Students', text: 'Plan your week, show your QR, rate meals and pay online.' },
  { icon: QrCode, title: 'Mess staff', text: 'Scan passes, watch the live headcount and update the menu.' },
  { icon: BarChart3, title: 'Admins', text: 'Approve students, set prices, generate bills and read the numbers.' },
]

const fade = { hidden: { opacity: 0, y: 24 }, show: { opacity: 1, y: 0 } }

export default function LandingPage() {
  const user = useAuthStore((s) => s.user)
  return (
    <>
      <MeshBackground />

      <header className="sticky top-0 z-30 px-4 pt-4">
        <nav className="glass mx-auto flex max-w-6xl items-center justify-between gap-2 !rounded-full px-3 py-2.5 sm:px-5">
          <Logo />
          <div className="hidden items-center gap-8 text-sm font-medium text-heading md:flex">
            <a href="#features" className="hover:text-accent">Features</a>
            <a href="#today" className="hover:text-accent">Today's menu</a>
            <a href="#roles" className="hover:text-accent">Who it's for</a>
          </div>
          <div className="flex items-center gap-2">
            <ThemeToggle className="!h-9 !w-9" />
            {user ? (
              <Link to={ROLE_HOME[user.role]} className={buttonClass('primary', '!px-4 !py-2')}>
                Open app
              </Link>
            ) : (
              <>
                <Link to="/login" className={buttonClass('ghost', '!px-3 !py-2 sm:!px-4')}>Log in</Link>
                <Link to="/register" className={buttonClass('primary', '!px-3 !py-2 sm:!px-4')}>Register</Link>
              </>
            )}
          </div>
        </nav>
      </header>

      <main id="top" className="mx-auto max-w-6xl px-4">
        <section className="grid items-center gap-12 py-16 md:grid-cols-2 md:py-24">
          <motion.div initial="hidden" animate="show" transition={{ staggerChildren: 0.12 }}>
            <motion.div variants={fade}><ServerStatus /></motion.div>
            <motion.h1 variants={fade} className="mt-6 text-5xl font-bold leading-[1.05] md:text-6xl">
              Your mess, <span className="text-accent">made effortless.</span>
            </motion.h1>
            <motion.p variants={fade} className="mt-5 max-w-lg text-lg text-ink-soft">
              Book meals, skip the queue with a QR pass, rate your food and pay your bill — built for the students and mess staff of IIIT Bhubaneswar.
            </motion.p>
            <motion.div variants={fade} className="mt-8 flex flex-wrap gap-3">
              <Link to={user ? ROLE_HOME[user.role] : '/register'} className={buttonClass('accent')}>
                {user ? 'Open my dashboard' : 'Get started'} <ArrowRight size={16} />
              </Link>
              <a href="#features" className={buttonClass('ghost')}>See what's inside</a>
            </motion.div>
          </motion.div>

          <motion.div id="today" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} transition={{ delay: 0.2, duration: 0.6 }}>
            <GlassCard strong hover={false} className="p-6">
              <div className="mb-4 flex items-center justify-between">
                <h2 className="text-xl font-bold">Today's menu</h2>
                <span className="rounded-full bg-primary/15 px-3 py-1 text-xs font-semibold text-primary">Sample</span>
              </div>
              <ul className="space-y-3">
                {SAMPLE.map((m) => {
                  const meta = MEAL_META[m.type]
                  return (
                    <li key={m.type} className={`flex items-center justify-between rounded-2xl bg-gradient-to-r p-4 ring-1 ring-white/20 ${meta.tint}`}>
                      <div>
                        <p className="font-semibold text-heading">{meta.label}</p>
                        <p className="text-sm text-ink-soft">{m.items}</p>
                      </div>
                      <span className="text-xs font-medium text-accent">{m.time}</span>
                    </li>
                  )
                })}
              </ul>
            </GlassCard>
          </motion.div>
        </section>

        <section id="features" className="py-16">
          <h2 className="text-center text-3xl font-bold md:text-4xl">Everything the mess needs, in one place</h2>
          <motion.div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4" initial="hidden" whileInView="show" viewport={{ once: true, margin: '-80px' }} transition={{ staggerChildren: 0.07 }}>
            {FEATURES.map(({ icon: Icon, title, text }) => (
              <motion.div key={title} variants={fade}>
                <GlassCard className="h-full p-6">
                  <span className="grid h-11 w-11 place-items-center rounded-2xl bg-primary text-on-primary"><Icon size={20} /></span>
                  <h3 className="mt-4 text-lg font-bold">{title}</h3>
                  <p className="mt-1 text-sm leading-relaxed text-ink-soft">{text}</p>
                </GlassCard>
              </motion.div>
            ))}
          </motion.div>
        </section>

        <section id="roles" className="py-16">
          <div className="grid gap-5 md:grid-cols-3">
            {ROLES.map(({ icon: Icon, title, text }) => (
              <GlassCard key={title} className="p-7">
                <Icon className="text-accent" size={28} />
                <h3 className="mt-4 text-xl font-bold">{title}</h3>
                <p className="mt-2 text-ink-soft">{text}</p>
              </GlassCard>
            ))}
          </div>
        </section>
      </main>

      <footer className="px-4 pb-8 pt-10 text-center text-sm text-ink-soft">
        © {new Date().getFullYear()} Thali · IIIT Bhubaneswar Mess Management
      </footer>
    </>
  )
}
