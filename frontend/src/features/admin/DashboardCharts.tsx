import { Area, AreaChart, Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useRevenue, useTrend, useWastage } from '@/api/ops'
import { GlassCard } from '@/components/ui/GlassCard'
import { formatINR } from '@/lib/format'

const axis = { stroke: 'rgb(var(--ink-soft))', fontSize: 12 }
const tip = { contentStyle: { background: 'var(--glass-strong-bg)', border: '1px solid var(--glass-border)', borderRadius: 12, backdropFilter: 'blur(8px)' } }

export default function DashboardCharts() {
  const trend = useTrend(30), revenue = useRevenue(), waste = useWastage()
  return (
    <div className="mt-6 grid gap-4 lg:grid-cols-2">
      <GlassCard hover={false} className="p-5"><h2 className="mb-3 text-lg font-bold">Meals served, last 30 days</h2>
        <div className="h-64" role="img" aria-label="Area chart of meals booked and served per day"><ResponsiveContainer><AreaChart data={trend.data?.map((d) => ({ ...d, day: d.date.slice(5) }))}><CartesianGrid strokeOpacity={0.15} /><XAxis dataKey="day" {...axis} interval={4} /><YAxis {...axis} /><Tooltip {...tip} /><Legend />
          <Area dataKey="booked" name="Booked" stroke="rgb(var(--primary))" fill="rgb(var(--primary))" fillOpacity={0.15} /><Area dataKey="served" name="Served" stroke="rgb(var(--accent))" fill="rgb(var(--accent))" fillOpacity={0.25} /></AreaChart></ResponsiveContainer></div></GlassCard>
      <GlassCard hover={false} className="p-5"><h2 className="mb-3 text-lg font-bold">Billing by month</h2>
        <div className="h-64" role="img" aria-label="Bar chart of billed and collected amounts per month"><ResponsiveContainer><BarChart data={revenue.data}><CartesianGrid strokeOpacity={0.15} /><XAxis dataKey="month" {...axis} /><YAxis {...axis} tickFormatter={(v) => `₹${v / 1000}k`} /><Tooltip {...tip} formatter={(v) => formatINR(Number(v))} /><Legend />
          <Bar dataKey="billed" name="Billed" fill="rgb(var(--primary))" radius={[6, 6, 0, 0]} /><Bar dataKey="collected" name="Collected" fill="rgb(var(--accent))" radius={[6, 6, 0, 0]} /></BarChart></ResponsiveContainer></div></GlassCard>
      <GlassCard hover={false} className="p-5 lg:col-span-2"><h2 className="text-lg font-bold">Food wasted (no-shows)</h2>
        <p className="mt-1 text-sm text-ink-soft">{waste.data ? `${waste.data.noShowRatePercent}% of booked meals weren't eaten, worth ${formatINR(waste.data.lostValue)} in the last 30 days.` : 'Loading…'}</p></GlassCard>
    </div>
  )
}
