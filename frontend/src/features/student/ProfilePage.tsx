import { useState } from 'react'
import { toast } from 'sonner'
import { useChangePassword, useProfile, useUpdateProfile } from '@/api/student'
import { Alert } from '@/components/ui/Alert'
import { Button } from '@/components/ui/Button'
import { GlassCard } from '@/components/ui/GlassCard'
import { Input, PasswordInput } from '@/components/ui/Input'
import { PageHeader } from '@/components/ui/PageHeader'
import { Skeleton } from '@/components/ui/Skeleton'
import { errorMessage } from '@/lib/errors'
import { password as passwordRule, phone as phoneRule } from '@/lib/validation'
import { useAuthStore } from '@/store/authStore'
import type { DietPref, User } from '@/types/api'

function Details({ u }: { u: User }) {
  const setUser = useAuthStore((s) => s.setUser)
  const save = useUpdateProfile()
  const [f, setF] = useState({ name: u.name, phone: u.phone ?? '', hostelBlock: u.hostelBlock ?? '', roomNo: u.roomNo ?? '', dietPref: (u.dietPref ?? 'VEG') as DietPref })
  const student = u.role === 'STUDENT'
  const phoneErr = f.phone && !phoneRule.safeParse(f.phone).success ? 'Enter a valid phone number' : undefined
  return (
    <GlassCard hover={false} className="p-6">
      <h2 className="mb-4 text-xl font-bold">Your details</h2>
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); save.mutateAsync({ ...f, phone: f.phone || null }).then((nu) => { setUser(nu); toast.success('Profile saved') }).catch((er) => toast.error(errorMessage(er))) }}>
        <Input label="Email" value={u.email} readOnly disabled hint={u.rollNo ? `Roll number ${u.rollNo}` : undefined} />
        <Input label="Full name" value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} />
        <Input label="Phone" value={f.phone} error={phoneErr} onChange={(e) => setF({ ...f, phone: e.target.value })} />
        {student && <div className="grid gap-3 sm:grid-cols-2"><Input label="Hostel block" value={f.hostelBlock} onChange={(e) => setF({ ...f, hostelBlock: e.target.value })} /><Input label="Room number" value={f.roomNo} onChange={(e) => setF({ ...f, roomNo: e.target.value })} /></div>}
        {student && <label className="block text-sm font-semibold text-heading">Food preference
          <select className="field mt-1" value={f.dietPref} onChange={(e) => setF({ ...f, dietPref: e.target.value as DietPref })}><option value="VEG">Vegetarian</option><option value="NON_VEG">Non-veg</option><option value="JAIN">Jain</option></select></label>}
        <Button type="submit" loading={save.isPending} disabled={!f.name.trim() || !!phoneErr}>Save changes</Button>
      </form>
    </GlassCard>
  )
}

function PasswordCard() {
  const change = useChangePassword()
  const [cur, setCur] = useState(''), [next, setNext] = useState('')
  const rule = next ? passwordRule.safeParse(next) : null
  return (
    <GlassCard hover={false} className="p-6">
      <h2 className="mb-4 text-xl font-bold">Change password</h2>
      <form className="space-y-3" onSubmit={(e) => { e.preventDefault(); change.mutateAsync({ currentPassword: cur, newPassword: next }).then(() => { toast.success('Password changed'); setCur(''); setNext('') }) }}>
        <PasswordInput label="Current password" autoComplete="current-password" value={cur} onChange={(e) => setCur(e.target.value)} />
        <PasswordInput label="New password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} error={rule && !rule.success ? rule.error.issues[0]?.message : undefined} hint="8+ characters with a letter and a number." />
        {change.error && <Alert tone="error">{errorMessage(change.error)}</Alert>}
        <Button type="submit" loading={change.isPending} disabled={!cur || !rule?.success}>Update password</Button>
      </form>
    </GlassCard>
  )
}

export default function ProfilePage() {
  const q = useProfile()
  return (
    <>
      <PageHeader title="Profile" />
      <div className="grid gap-6 lg:grid-cols-2">{q.data ? <Details u={q.data} /> : <Skeleton className="h-80" />}<PasswordCard /></div>
    </>
  )
}
