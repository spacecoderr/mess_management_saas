import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { get, http, post } from './client'
import type { AdminBill, AdminFeedback, AdminUser, Announcement, ApprovalStatus, DailyMenu, DayPoint, Dish, DishScore, FeedbackSummary, MealSlot, MealType, Page, Payment, RevenuePoint, ScanResult, StaffBooking, StaffComplaint, Student } from '@/types/api'

const qs = (o: Record<string, string | number | boolean | undefined>) => {
  const p = new URLSearchParams()
  Object.entries(o).forEach(([k, v]) => v !== undefined && v !== '' && p.set(k, String(v)))
  const s = p.toString()
  return s ? `?${s}` : ''
}
function useDo<V, R>(fn: (v: V) => Promise<R>, invalidate: string[]) {
  const qc = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => Promise.all(invalidate.map((k) => qc.invalidateQueries({ queryKey: [k] }))) })
}

// ---- staff: counter
export const useStaffBookings = (date: string, mealType: MealType, q: string) =>
  useQuery({ queryKey: ['staff-bookings', date, mealType, q], queryFn: () => get<StaffBooking[]>(`/bookings${qs({ date, mealType, q, status: 'BOOKED' })}`) })
export const useScan = () => useDo((qrToken: string) => post<ScanResult>('/attendance/scan', { qrToken }), ['attendance', 'staff-bookings'])
export const useManual = () => useDo((bookingId: number) => post<ScanResult>('/attendance/manual', { bookingId }), ['attendance', 'staff-bookings'])

// ---- dishes, menus
export const useDishes = () => useQuery({ queryKey: ['dishes'], queryFn: () => get<Dish[]>('/menu-items') })
export const useSaveDish = () => useDo((v: { id?: number; name: string; category: string; veg: boolean; description?: string; active?: boolean }) => (v.id ? http.put<Dish>(`/menu-items/${v.id}`, v) : http.post<Dish>('/menu-items', v)).then((r) => r.data), ['dishes'])
export const useRetireDish = () => useDo((id: number) => http.delete(`/menu-items/${id}`), ['dishes'])
export const useWeekMenus = (from: string, to: string) => useQuery({ queryKey: ['menus', 'admin', from, to], queryFn: () => get<DailyMenu[]>(`/menus?from=${from}&to=${to}`) })
export const useSetMenu = () => useDo((v: { date: string; mealType: MealType; itemIds: number[]; publish?: boolean }) => http.put<DailyMenu>(`/menus/${v.date}/${v.mealType}`, { itemIds: v.itemIds, publish: v.publish }).then((r) => r.data), ['menus'])
export const usePublish = () => useDo((v: { id: number; published: boolean }) => http.patch<DailyMenu>(`/menus/${v.id}/publish`, { published: v.published }).then((r) => r.data), ['menus'])
export const useCopyWeek = () => useDo((v: { fromWeekStart: string; toWeekStart: string }) => post<{ copied: number; skipped: number }>('/menus/copy-week', v), ['menus'])

// ---- complaints queue (staff + admin)
export const useQueue = (status: string, assignedToMe: boolean) => useQuery({ queryKey: ['complaints', 'queue', status, assignedToMe], queryFn: () => get<Page<StaffComplaint>>(`/complaints${qs({ status, assignedToMe, size: 50 })}`) })
export const useHandle = () => useDo((v: { id: number; message: string; newStatus?: string }) => post(`/complaints/${v.id}/updates`, { message: v.message, newStatus: v.newStatus }), ['complaints'])
export const useAssign = () => useDo((v: { id: number; staffId: number }) => http.patch(`/complaints/${v.id}/assign`, { staffId: v.staffId }).then((r) => r.data), ['complaints'])

// ---- admin
export const useStudents = (status: string, q: string) => useQuery({ queryKey: ['students', status, q], queryFn: () => get<Page<Student>>(`/students${qs({ status, q, size: 100 })}`) })
export const useDecide = () => useDo((v: { id: number; status: ApprovalStatus }) => http.patch<Student>(`/students/${v.id}/approval`, { status: v.status }).then((r) => r.data), ['students', 'analytics'])
export const useUsers = (role?: string) => useQuery({ queryKey: ['users', role], queryFn: () => get<Page<AdminUser>>(`/users${qs({ role, size: 100 })}`) })
export const useCreateUser = () => useDo((v: { name: string; email: string; role: string; password: string; phone?: string }) => post<AdminUser>('/users', v), ['users'])
export const useSetActive = () => useDo((v: { id: number; active: boolean }) => http.patch(`/users/${v.id}/status`, { active: v.active }).then((r) => r.data), ['users'])
export const useSlots = () => useQuery({ queryKey: ['slots'], queryFn: () => get<MealSlot[]>('/meal-slots') })
export const useSaveSlot = () => useDo((s: MealSlot) => http.put<MealSlot>(`/meal-slots/${s.mealType}`, s).then((r) => r.data), ['slots', 'menus'])
export const useAdminBills = (month: string, status: string) => useQuery({ queryKey: ['bills', 'admin', month, status], queryFn: () => get<Page<AdminBill>>(`/bills${qs({ month, status, size: 100 })}`) })
export const useGenerate = () => useDo((month: string) => post<{ created: number; updated: number; skippedPaid: number }>(`/bills/generate?month=${month}`), ['bills', 'analytics'])
export const useCash = () => useDo((v: { billId: number; amount: number }) => post<Payment>('/payments/cash', v), ['bills', 'analytics'])
export const usePostNotice = () => useDo((v: { title: string; body: string; pinned: boolean }) => post<Announcement>('/announcements', v), ['announcements'])
export const useDeleteNotice = () => useDo((id: number) => http.delete(`/announcements/${id}`), ['announcements'])

// ---- analytics
export const useTrend = (days: number) => useQuery({ queryKey: ['analytics', 'trend', days], queryFn: () => get<DayPoint[]>(`/analytics/bookings-trend?days=${days}`) })
export const useRevenue = () => useQuery({ queryKey: ['analytics', 'revenue'], queryFn: () => get<RevenuePoint[]>('/analytics/revenue?months=6') })
export const useDishScores = (order: 'best' | 'worst') => useQuery({ queryKey: ['analytics', 'dishes', order], queryFn: () => get<DishScore[]>(`/analytics/top-dishes?order=${order}&limit=5`) })
export const useWastage = () => useQuery({ queryKey: ['analytics', 'wastage'], queryFn: () => get<{ noShowRatePercent: number; noShow: number; lostValue: number; byMeal: { mealType: MealType; noShowRatePercent: number }[] }>('/analytics/wastage') })
export const useFeedbackSummary = () => useQuery({ queryKey: ['analytics', 'feedback'], queryFn: () => get<FeedbackSummary>('/feedback/summary') })
export const useFeedbackList = () => useQuery({ queryKey: ['analytics', 'feedback-list'], queryFn: () => get<Page<AdminFeedback>>('/feedback?size=30') })
export const useStaffList = () => useUsers('STAFF')

/** Downloads a CSV through the authenticated client (a plain link couldn't send the token). */
export async function downloadCsv(path: string, filename: string) {
  const res = await http.get<Blob>(path, { responseType: 'blob' })
  const url = URL.createObjectURL(res.data)
  const a = Object.assign(document.createElement('a'), { href: url, download: filename })
  a.click()
  URL.revokeObjectURL(url)
}
