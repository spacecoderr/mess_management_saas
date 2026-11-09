import { useState } from 'react'
import { Inbox } from 'lucide-react'
import { toast } from 'sonner'
import { useAssign, useHandle, useQueue, useStaffList } from '@/api/ops'
import { useComplaint } from '@/api/student'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import { useAuthStore } from '@/store/authStore'
import type { ComplaintStatus } from '@/types/api'

const STATUSES: ComplaintStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED']
const LABEL: Record<ComplaintStatus, string> = { OPEN: 'Open', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', REJECTED: 'Rejected' }
const TONE: Record<ComplaintStatus, 'accent' | 'primary' | 'success' | 'neutral'> = { OPEN: 'accent', IN_PROGRESS: 'primary', RESOLVED: 'success', REJECTED: 'neutral' }

function Detail({ id, onClose }: { id: number; onClose: () => void }) {
  const admin = useAuthStore((s) => s.user!.role === 'ADMIN')
  const q = useComplaint(id), handle = useHandle(), assign = useAssign(), staff = useStaffList()
  const [msg, setMsg] = useState(''), [status, setStatus] = useState<string>('')
  const c = q.data
  return (
    <Modal open onClose={onClose} title={c?.title ?? 'Complaint'}>
      {!c ? <Skeleton className="h-40" /> : (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center gap-2"><Badge tone={TONE[c.status]}>{LABEL[c.status]}</Badge><span className="text-xs text-ink-soft">{(c as unknown as { studentName: string }).studentName} · {c.category.replace('_', ' ').toLowerCase()} · {c.priority.toLowerCase()} priority</span></div>
          <p className="text-sm">{c.description}</p>
          {admin && <label className="block text-sm font-semibold text-heading">Assign to
            <select className="field mt-1" value="" onChange={(e) => e.target.value && assign.mutateAsync({ id, staffId: Number(e.target.value) }).then(() => toast.success('Assigned')).catch((er) => toast.error(errorMessage(er)))}>
              <option value="">{c.assignedToName ?? 'Choose staff…'}</option>{staff.data?.content.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}</select></label>}
          <ol className="space-y-2 border-l-2 pl-4" style={{ borderColor: 'var(--glass-border)' }} aria-label="Timeline">{c.updates?.map((u) => <li key={u.id} className="text-sm"><strong className="text-heading">{u.authorName}</strong>{u.newStatus && <> → {LABEL[u.newStatus]}</>}<br />{u.message}</li>)}</ol>
          <form className="space-y-2" onSubmit={(e) => { e.preventDefault(); handle.mutateAsync({ id, message: msg, newStatus: status || undefined }).then(() => { setMsg(''); setStatus(''); toast.success('Update sent') }).catch((er) => toast.error(errorMessage(er))) }}>
            <input aria-label="Reply" className="field" placeholder="Write an update for the student" value={msg} onChange={(e) => setMsg(e.target.value)} />
            <div className="flex gap-2"><select aria-label="New status" className="field" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">Keep status</option>{STATUSES.map((s) => <option key={s} value={s}>{LABEL[s]}</option>)}</select><Button type="submit" disabled={!msg.trim()} loading={handle.isPending}>Send</Button></div></form>
        </div>)}
    </Modal>
  )
}

export default function ComplaintsQueue() {
  const admin = useAuthStore((s) => s.user!.role === 'ADMIN')
  const [status, setStatus] = useState(''), [mine, setMine] = useState(false), [open, setOpen] = useState<number | null>(null)
  const q = useQueue(status, mine)
  return (
    <>
      <PageHeader title="Complaints" subtitle={admin ? 'All complaints. Assign and resolve.' : 'Complaints from students. Reply and update status.'} />
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <select aria-label="Filter by status" className="field !w-auto" value={status} onChange={(e) => setStatus(e.target.value)}><option value="">All statuses</option>{STATUSES.map((s) => <option key={s} value={s}>{LABEL[s]}</option>)}</select>
        <label className="flex items-center gap-2 text-sm font-semibold text-heading"><input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} /> Assigned to me</label></div>
      {q.isLoading ? <Skeleton className="h-40" /> : q.data?.content.length ? <div className="space-y-3">{q.data.content.map((c) => (
        <button key={c.id} type="button" className="block w-full text-left" onClick={() => setOpen(c.id)}><GlassCard className="flex items-center justify-between gap-3 p-4"><div><p className="font-bold text-heading">{c.title}</p><p className="text-sm text-ink-soft">{c.studentName} · {c.rollNo} · {c.assignedToName ? `with ${c.assignedToName}` : 'unassigned'}</p></div><Badge tone={TONE[c.status]}>{LABEL[c.status]}</Badge></GlassCard></button>))}</div>
        : <EmptyState icon={Inbox} title="Nothing here" text="No complaints match these filters." />}
      {open !== null && <Detail id={open} onClose={() => setOpen(null)} />}
    </>
  )
}
