import { useQuery } from '@tanstack/react-query'
import { get } from './client'
import type { Announcement, Bill, DailyMenu, LiveCounts, Overview } from '@/types/api'

export const useTodayMenus = () => useQuery({ queryKey: ['menus', 'today'], queryFn: () => get<DailyMenu[]>('/menus/today') })
export const useAnnouncements = () => useQuery({ queryKey: ['announcements'], queryFn: () => get<Announcement[]>('/announcements') })
export const useMyBills = () => useQuery({ queryKey: ['bills', 'me'], queryFn: () => get<Bill[]>('/bills/me') })
export const useOverview = () => useQuery({ queryKey: ['analytics', 'overview'], queryFn: () => get<Overview>('/analytics/overview') })

/** Polled every 15 s so the counter screen stays live without websockets. */
export const useLiveCounts = () =>
  useQuery({ queryKey: ['attendance', 'live'], queryFn: () => get<LiveCounts[]>('/attendance/live'), refetchInterval: 15_000 })
