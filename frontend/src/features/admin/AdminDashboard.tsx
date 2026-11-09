import { lazy, Suspense } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, CalendarCheck, GraduationCap, HandCoins, MessageSquareWarning, Star, Ticket, UserCheck, Wallet } from 'lucide-react'
import { useOverview } from '@/api/queries'
import { Alert } from '@/components/ui/Alert'
import { PageHeader } from '@/components/ui/PageHeader'
import { StatTile } from '@/components/ui/StatTile'
import { formatINR } from '@/lib/format'
import { errorMessage } from '@/lib/errors'
import { Skeleton } from '@/components/ui/Skeleton'

const DashboardCharts = lazy(() => import('./DashboardCharts'))

export default function AdminDashboard() {
  const q = useOverview()
  const o = q.data
  const rupees = (n: number) => formatINR(n)

  return (
    <>
      <PageHeader title="Dashboard" subtitle="How the mess is doing right now." />
      {q.isError && <Alert tone="error" className="mb-4">{errorMessage(q.error)}</Alert>}
      {o && o.pendingApprovals > 0 && (
        <Alert tone="info" className="mb-4">
          <span className="font-semibold">{o.pendingApprovals} student{o.pendingApprovals > 1 ? 's are' : ' is'} waiting for approval.</span>{' '}
          <Link to="/app/students" className="underline">
            Review
          </Link>
        </Alert>
      )}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatTile label="Approved students" icon={GraduationCap} value={o?.approvedStudents} loading={q.isLoading} />
        <StatTile label="Pending approvals" icon={UserCheck} value={o?.pendingApprovals} loading={q.isLoading} />
        <StatTile label="Today's bookings" icon={Ticket} value={o?.todayBookings} loading={q.isLoading} />
        <StatTile label="Served today" icon={CalendarCheck} value={o?.todayServed} loading={q.isLoading} />
        <StatTile label="Revenue this month" icon={Wallet} value={o?.revenueThisMonth} format={rupees} loading={q.isLoading} />
        <StatTile label="Outstanding dues" icon={HandCoins} value={o?.outstanding} format={rupees} loading={q.isLoading} />
        <StatTile label="Overdue bills" icon={AlertTriangle} value={o?.overdueBills} loading={q.isLoading} />
        <StatTile label="Open complaints" icon={MessageSquareWarning} value={o?.openComplaints} loading={q.isLoading} />
        <StatTile label="Average rating (30 days)" icon={Star} value={o?.averageRating30d ?? undefined} format={(n) => n.toFixed(2)} loading={q.isLoading} empty="No ratings yet" />
      </div>
      <Suspense fallback={<Skeleton className="mt-6 h-64" />}><DashboardCharts /></Suspense>
    </>
  )
}
