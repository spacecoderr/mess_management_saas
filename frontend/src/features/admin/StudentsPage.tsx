import { useState } from 'react'
import { toast } from 'sonner'
import { useDecide, useStudents } from '@/api/ops'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { Tabs } from '@/components/ui/Tabs'
import { errorMessage } from '@/lib/errors'
import { GraduationCap } from 'lucide-react'

type F = 'PENDING' | 'APPROVED' | 'REJECTED' | ''
const TONE = { PENDING: 'accent', APPROVED: 'success', REJECTED: 'danger' } as const

export default function StudentsPage() {
  const [status, setStatus] = useState<F>('PENDING'), [q, setQ] = useState('')
  const list = useStudents(status, q), decide = useDecide()
  const act = (id: number, s: 'APPROVED' | 'REJECTED') => decide.mutateAsync({ id, status: s }).then(() => toast.success(s === 'APPROVED' ? 'Student approved' : 'Student rejected')).catch((e) => toast.error(errorMessage(e)))
  return (
    <>
      <PageHeader title="Students" subtitle="Approve new registrations and look up students." />
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Tabs<F> label="Status" value={status} onChange={setStatus} options={[{ value: 'PENDING', label: 'Pending' }, { value: 'APPROVED', label: 'Approved' }, { value: 'REJECTED', label: 'Rejected' }, { value: '', label: 'All' }]} />
        <input aria-label="Search students" className="field !w-64" placeholder="Name, roll or email" value={q} onChange={(e) => setQ(e.target.value)} /></div>
      {list.isLoading ? <Skeleton className="h-48" /> : list.data?.content.length ? (
        <div className="glass overflow-x-auto !rounded-2xl"><table className="w-full min-w-[40rem] text-left text-sm"><thead className="text-ink-soft"><tr><th className="p-3">Student</th><th>Roll</th><th>Hostel</th><th>Status</th><th /></tr></thead>
          <tbody>{list.data.content.map((s) => (<tr key={s.userId} className="border-t" style={{ borderColor: 'var(--glass-border)' }}>
            <td className="p-3"><p className="font-bold text-heading">{s.name}</p><p className="text-xs text-ink-soft">{s.email}</p></td><td>{s.rollNo}</td><td>{s.hostelBlock} {s.roomNo}</td><td><Badge tone={TONE[s.approvalStatus]}>{s.approvalStatus.toLowerCase()}</Badge></td>
            <td className="space-x-2 p-3 text-right">{s.approvalStatus !== 'APPROVED' && <Button className="!px-3 !py-1.5" onClick={() => act(s.userId, 'APPROVED')}>Approve</Button>}{s.approvalStatus !== 'REJECTED' && <Button variant="ghost" className="!px-3 !py-1.5" onClick={() => act(s.userId, 'REJECTED')}>Reject</Button>}</td></tr>))}</tbody></table></div>
      ) : <EmptyState icon={GraduationCap} title={status === 'PENDING' ? 'No one is waiting' : 'No students found'} text="New registrations appear here for approval." />}
    </>
  )
}
