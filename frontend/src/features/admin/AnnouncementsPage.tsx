import { useState } from 'react'
import { Pin, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { useAnnouncements } from '@/api/queries'
import { useDeleteNotice, usePostNotice } from '@/api/ops'
import { Button } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { Input } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { errorMessage } from '@/lib/errors'

export default function AnnouncementsPage() {
  const list = useAnnouncements(), post = usePostNotice(), del = useDeleteNotice()
  const [f, setF] = useState({ title: '', body: '', pinned: false })
  return (
    <>
      <PageHeader title="Announcements" subtitle="Notices shown on every student's home page." />
      <div className="grid gap-6 lg:grid-cols-2">
        <GlassCard hover={false} className="p-5"><form className="space-y-3" onSubmit={(e) => { e.preventDefault(); post.mutateAsync(f).then(() => { toast.success('Posted'); setF({ title: '', body: '', pinned: false }) }).catch((er) => toast.error(errorMessage(er))) }}>
          <Input label="Title" maxLength={150} value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} />
          <label className="block text-sm font-semibold text-heading">Message<textarea className="field mt-1" rows={4} maxLength={2000} value={f.body} onChange={(e) => setF({ ...f, body: e.target.value })} /></label>
          <label className="flex items-center gap-2 text-sm font-semibold text-heading"><input type="checkbox" checked={f.pinned} onChange={(e) => setF({ ...f, pinned: e.target.checked })} /> Pin to the top</label>
          <Button type="submit" disabled={!f.title.trim() || !f.body.trim()} loading={post.isPending}>Post announcement</Button></form></GlassCard>
        <ul className="space-y-3">{list.data?.map((a) => <li key={a.id} className="glass flex items-start justify-between gap-3 !rounded-2xl p-4"><div><p className="flex items-center gap-1 font-bold text-heading">{a.pinned && <Pin size={14} className="text-accent" aria-label="pinned" />}{a.title}</p><p className="text-sm text-ink-soft">{a.body}</p></div>
          <button type="button" aria-label={`Delete ${a.title}`} className="rounded-full p-2 text-ink-soft hover:text-danger" onClick={() => del.mutateAsync(a.id).then(() => toast.success('Deleted'))}><Trash2 size={16} /></button></li>)}</ul></div>
    </>
  )
}
