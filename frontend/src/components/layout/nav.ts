import {
  CalendarDays, CalendarRange, ClipboardCheck, House, LayoutDashboard, Megaphone, MessageSquareWarning, Receipt, ScanLine,
  SlidersHorizontal, Soup, Star, Ticket, UserCog, UserRound, Users, UtensilsCrossed, type LucideIcon,
} from 'lucide-react'
import type { Role } from '@/types/api'

export interface NavItem {
  to: string
  label: string
  icon: LucideIcon
  /** which build phase delivers the real page (placeholder text only) */
  phase?: number
}

/** The sidebar (desktop), bottom bar (first four items) and drawer (everything) all read this one list. */
export const NAV: Record<Role, NavItem[]> = {
  STUDENT: [
    { to: '/app/home', label: 'Home', icon: House },
    { to: '/app/menu', label: 'Menu', icon: CalendarDays },
    { to: '/app/bookings', label: 'My bookings', icon: Ticket },
    { to: '/app/bills', label: 'Bills', icon: Receipt },
    { to: '/app/feedback', label: 'Feedback', icon: Star },
    { to: '/app/complaints', label: 'Complaints', icon: MessageSquareWarning },
    { to: '/app/profile', label: 'Profile', icon: UserRound },
  ],
  STAFF: [
    { to: '/app/serve', label: 'Serve counter', icon: ScanLine },
    { to: '/app/menu-today', label: "Today's menu", icon: UtensilsCrossed, phase: 7 },
    { to: '/app/complaints', label: 'Complaints', icon: MessageSquareWarning },
    { to: '/app/profile', label: 'Profile', icon: UserRound },
  ],
  ADMIN: [
    { to: '/app/dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { to: '/app/students', label: 'Students', icon: Users },
    { to: '/app/billing', label: 'Billing', icon: Receipt },
    { to: '/app/complaints', label: 'Complaints', icon: MessageSquareWarning },
    { to: '/app/menu-builder', label: 'Menu builder', icon: CalendarRange },
    { to: '/app/dishes', label: 'Dishes', icon: Soup },
    { to: '/app/meal-settings', label: 'Meal settings', icon: SlidersHorizontal },
    { to: '/app/attendance', label: 'Attendance', icon: ClipboardCheck },
    { to: '/app/feedback', label: 'Feedback', icon: Star },
    { to: '/app/staff', label: 'Staff', icon: UserCog },
    { to: '/app/announcements', label: 'Announcements', icon: Megaphone },
    { to: '/app/profile', label: 'Profile', icon: UserRound },
  ],
}
