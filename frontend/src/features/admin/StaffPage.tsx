import { useState } from 'react'
import { Plus } from 'lucide-react'
import { toast } from 'sonner'
import { useCreateUser, useSetActive, useUsers } from '@/api/ops'
import { Alert } from '@/components/ui/Alert'
import { Badge } from '@/components/ui/Badge'
import { Button } from '@/components/ui/Button'
import { Input, PasswordInput } from '@/components/ui/Input'
import { Modal } from '@/components/ui/Modal'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import { password as pw } from '@/lib/validation'
import { useAuthStore } from '@/store/authStore'

export default function StaffPage() {
  const me = useAuthStore((s) => s.user!.id)
  const staff = useUsers('STAFF'), admins = useUsers('ADMIN'), create = useCreateUser(), toggle = useSetActive()
  const [open, setOpen] = useState(false)
  const [f, setF] = useState({ name: '', email: '', password: '', role: 'STAFF' })
  const rows = [...(staff.data?.content ?? []), ...(admins.data?.content ?? [])]
  const valid = f.name.trim() && /\S+@\S+\.\S+/.test(f.email) && pw.safeParse(f.password).success
  return (
    <>
      <PageHeader title="Staff & admins" subtitle="Mess staff log in with email and password." action={<Button onClick={() => setOpen(true)}><Plus size={16} /> Add staff</Button>} />
      {staff.isLoading ? <Skeleton className="h-40" /> : (
        <ul className="space-y-2">{rows.map((u) => (<li key={u.id} className="glass flex flex-wrap items-center justify-between gap-3 !rounded-2xl px-4 py-3">
          <div><p className="font-bold text-heading">{u.name} <Badge tone={u.role === 'ADMIN' ? 'accent' : 'primary'}>{u.role.toLowerCase()}</Badge> {!u.active && <Badge tone="danger">disabled</Badge>}</p><p className="text-sm text-ink-soft">{u.email}</p></div>
          {u.id !== me && <Button variant="ghost" className="!py-2" onClick={() => toggle.mutateAsync({ id: u.id, active: !u.active }).then(() => toast.success(u.active ? 'Disabled' : 'Enabled')).catch((e) => toast.error(errorMessage(e)))}>{u.active ? 'Disable' : 'Enable'}</Button>}</li>))}</ul>)}
      <Modal open={open} onClose={() => setOpen(false)} title="Add a staff member">
        <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); create.mutateAsync(f).then(() => { toast.success('Account created'); setOpen(false); setF({ name: '', email: '', password: '', role: 'STAFF' }) }) }}>
          <Input label="Full name" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />
          <Input label="Email" type="email" value={f.email} onChange={(e) => setF({ ...f, email: e.target.value })} />
          <label className="block text-sm font-semibold text-heading">Role<select className="field mt-1" value={f.role} onChange={(e) => setF({ ...f, role: e.target.value })}><option value="STAFF">Staff</option><option value="ADMIN">Admin</option></select></label>
          <PasswordInput label="Temporary password" autoComplete="new-password" hint="8+ characters with a letter and a number." value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} />
          {create.error && <Alert tone="error">{errorMessage(create.error)}</Alert>}
          <Button type="submit" className="w-full" disabled={!valid} loading={create.isPending}>Create account</Button></form></Modal>
    </>
  )
}
