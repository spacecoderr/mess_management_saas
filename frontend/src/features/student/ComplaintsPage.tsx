import { useState } from 'react'
import { MessageSquareWarning, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { useComment, useComplaint, useMyComplaints, useRaise } from '@/api/student'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { EmptyState } from '@/components/ui/EmptyState'
import { GlassCard } from '@/components/ui/GlassCard'
import { Input } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import type { ComplaintCategory, ComplaintStatus } from '@/types/api'

const CATS: { value: ComplaintCategory; label: string }[] = [
  { value: 'FOOD_QUALITY', label: 'Food quality' }, { value: 'HYGIENE', label: 'Hygiene' }, { value: 'SERVICE', label: 'Service' }, { value: 'BILLING', label: 'Billing' }, { value: 'OTHER', label: 'Other' }]
const TONE: Record<ComplaintStatus, 'accent' | 'primary' | 'success' | 'neutral'> = { OPEN: 'accent', IN_PROGRESS: 'primary', RESOLVED: 'success', REJECTED: 'neutral' }
const STATUS: Record<ComplaintStatus, string> = { OPEN: 'Open', IN_PROGRESS: 'In progress', RESOLVED: 'Resolved', REJECTED: 'Rejected' }

function NewDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const raise = useRaise()
  const [f, setF] = useState({ category: 'FOOD_QUALITY' as ComplaintCategory, title: '', description: '', priority: 'MEDIUM' })
  const valid = f.title.trim().length > 0 && f.description.trim().length > 0
  return (
    <Modal open={open} onClose={onClose} title="New complaint">
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); raise.mutateAsync(f).then(() => { toast.success('Complaint submitted'); setF({ ...f, title: '', description: '' }); onClose() }) }}>
        <label className="block text-sm font-semibold text-heading">Category
          <select className="field mt-1" value={f.category} onChange={(e) => setF({ ...f, category: e.target.value as ComplaintCategory })}>{CATS.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}</select></label>
        <Input label="Title" maxLength={150} value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} />
        <label className="block text-sm font-semibold text-heading">Details
          <textarea className="field mt-1" rows={4} maxLength={2000} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></label>
        {raise.error && <Alert tone="error">{errorMessage(raise.error)}</Alert>}
        <Button type="submit" className="w-full" disabled={!valid} loading={raise.isPending}>Submit</Button>
      </form>
    </Modal>
  )
}

function DetailDialog({ id, onClose }: { id: number | null; onClose: () => void }) {
  const q = useComplaint(id), comment = useComment()
  const [msg, setMsg] = useState('')
  const c = q.data
  const closed = c?.status === 'RESOLVED' || c?.status === 'REJECTED'
  return (
    <Modal open={id !== null} onClose={onClose} title={c?.title ?? 'Complaint'}>
      {!c ? <Skeleton className="h-40" /> : (
        <div className="space-y-4">
          <div className="flex items-center gap-2"><Badge tone={TONE[c.status]}>{STATUS[c.status]}</Badge>{c.assignedToName && <span className="text-xs text-ink-soft">Handled by {c.assignedToName}</span>}</div>
          <p className="text-sm text-ink">{c.description}</p>
          <ol className="space-y-2 border-l-2 pl-4" style={{ borderColor: 'var(--glass-border)' }} aria-label="Timeline">
            {c.updates?.map((u) => <li key={u.id} className="text-sm"><span className="font-bold text-heading">{u.authorName}</span> <span className="text-ink-soft">({u.authorRole.toLowerCase()})</span>{u.newStatus && <> → <strong>{STATUS[u.newStatus]}</strong></>}<br />{u.message}</li>)}
          </ol>
          {closed ? <p className="text-sm text-ink-soft">This complaint is closed.</p> : (
            <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); comment.mutateAsync({ id: c.id, message: msg }).then(() => setMsg('')).catch((er) => toast.error(errorMessage(er))) }}>
              <input aria-label="Add a comment" className="field" value={msg} onChange={(e) => setMsg(e.target.value)} placeholder="Add a comment" /><Button type="submit" disabled={!msg.trim()} loading={comment.isPending}>Send</Button></form>)}
        </div>
      )}
    </Modal>
  )
}

export default function ComplaintsPage() {
  const list = useMyComplaints()
  const [creating, setCreating] = useState(false)
  const [open, setOpen] = useState<number | null>(null)
  return (
    <>
      <PageHeader title="Complaints" subtitle="Tell us what went wrong and follow it to resolution." action={<Button onClick={() => setCreating(true)}><Plus size={16} /> New complaint</Button>} />
      {list.isLoading ? <Skeleton className="h-32" /> : list.data?.length ? (
        <div className="space-y-3">{list.data.map((c) => (
          <button key={c.id} type="button" onClick={() => setOpen(c.id)} className="block w-full text-left">
            <GlassCard className="flex items-center justify-between gap-3 p-4"><div><p className="font-bold text-heading">{c.title}</p><p className="text-sm text-ink-soft">{CATS.find((x) => x.value === c.category)?.label}</p></div><Badge tone={TONE[c.status]}>{STATUS[c.status]}</Badge></GlassCard></button>))}</div>
      ) : <EmptyState icon={MessageSquareWarning} title="No complaints" text="Hopefully that means everything is fine!" />}
      <NewDialog open={creating} onClose={() => setCreating(false)} />
      <DetailDialog id={open} onClose={() => setOpen(null)} />
    </>
  )
}
