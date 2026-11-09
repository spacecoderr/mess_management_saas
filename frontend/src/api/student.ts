import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { get, http, post } from './client'
import type { BillDetail, Booking, BulkResult, Complaint, ComplaintCategory, DailyMenu, FeedbackItem, MealType, Payment, PendingFeedback, QrInfo, User } from '@/types/api'

export const useMenus = (from: string, to: string) => useQuery({ queryKey: ['menus', from, to], queryFn: () => get<DailyMenu[]>(`/menus?from=${from}&to=${to}`) })
export const useMyBookings = (from: string, to: string) => useQuery({ queryKey: ['bookings', 'me', from, to], queryFn: () => get<Booking[]>(`/bookings/me?from=${from}&to=${to}`) })
export const useQr = (id: number | null) => useQuery({ queryKey: ['qr', id], queryFn: () => get<QrInfo>(`/bookings/${id}/qr`), enabled: id !== null })
export const usePendingFeedback = () => useQuery({ queryKey: ['feedback', 'pending'], queryFn: () => get<PendingFeedback[]>('/feedback/me/pending') })
export const useMyFeedback = () => useQuery({ queryKey: ['feedback', 'me'], queryFn: () => get<FeedbackItem[]>('/feedback/me') })
export const useMyComplaints = () => useQuery({ queryKey: ['complaints', 'me'], queryFn: () => get<Complaint[]>('/complaints/me') })
export const useComplaint = (id: number | null) => useQuery({ queryKey: ['complaints', id], queryFn: () => get<Complaint>(`/complaints/${id}`), enabled: id !== null })
export const useBillDetail = (id: number | null) => useQuery({ queryKey: ['bills', id], queryFn: () => get<BillDetail>(`/bills/${id}`), enabled: id !== null })
export const useProfile = () => useQuery({ queryKey: ['profile'], queryFn: () => get<User>('/me/profile') })

/** Runs a mutation, then refreshes the lists it affects. */
function useAction<V, R>(fn: (v: V) => Promise<R>, invalidate: string[]) {
  const qc = useQueryClient()
  return useMutation({ mutationFn: fn, onSuccess: () => Promise.all(invalidate.map((k) => qc.invalidateQueries({ queryKey: [k] }))) })
}

export const useBook = () => useAction((v: { date: string; mealType: MealType }) => post<Booking>('/bookings', v), ['bookings', 'menus'])
export const useBookMany = () => useAction((items: { date: string; mealType: MealType }[]) => post<BulkResult>('/bookings/bulk', { items }), ['bookings', 'menus'])
export const useCancel = () => useAction((id: number) => http.delete<Booking>(`/bookings/${id}`).then((r) => r.data), ['bookings', 'menus'])
export const useRate = () => useAction((v: { dailyMenuId: number; rating: number; comment?: string }) => post('/feedback', v), ['feedback'])
export const useRaise = () => useAction((v: { category: ComplaintCategory; title: string; description: string; priority: string }) => post<Complaint>('/complaints', v), ['complaints'])
export const useComment = () => useAction((v: { id: number; message: string }) => post<Complaint>(`/complaints/${v.id}/updates`, { message: v.message }), ['complaints'])
export const useCheckout = () => useMutation({ mutationFn: (v: { billId: number; method: 'UPI' | 'CARD' }) => post<Payment>('/payments/checkout', v) })
export const useConfirmPayment = () => useAction((id: number) => post<Payment>(`/payments/${id}/confirm`), ['bills'])
export const useUpdateProfile = () => useAction((v: Partial<User> & { name: string }) => http.put<User>('/me/profile', v).then((r) => r.data), ['profile'])
export const useChangePassword = () => useMutation({ mutationFn: (v: { currentPassword: string; newPassword: string }) => http.put('/me/password', v).then((r) => r.data) })
