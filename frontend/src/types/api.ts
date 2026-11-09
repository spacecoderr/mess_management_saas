/** Mirrors the backend DTOs (see backend Swagger). Dates are ISO strings; money is a JSON number. */
export type Role = 'ADMIN' | 'STAFF' | 'STUDENT'
export type ApprovalStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type DietPref = 'VEG' | 'NON_VEG' | 'JAIN'
export type MealType = 'BREAKFAST' | 'LUNCH' | 'SNACKS' | 'DINNER'
export type OtpPurpose = 'REGISTER' | 'LOGIN' | 'RESET_PASSWORD'

export interface User {
  id: number
  name: string
  email: string
  role: Role
  phone: string | null
  emailVerified: boolean
  approvalStatus: ApprovalStatus | null
  rollNo: string | null
  batchYear: number | null
  hostelBlock: string | null
  roomNo: string | null
  dietPref: DietPref | null
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  user: User
}

export interface OtpVerifyResponse {
  registrationToken: string | null
  auth: AuthResponse | null
}

export interface Dish {
  id: number
  name: string
  description: string | null
  category: 'MAIN' | 'SIDE' | 'BREAD' | 'RICE' | 'DESSERT' | 'BEVERAGE'
  veg: boolean
  imageUrl: string | null
  active: boolean
}

export interface DailyMenu {
  id: number
  date: string
  mealType: MealType
  published: boolean
  items: Dish[]
  startTime: string
  endTime: string
  price: number
  bookingClosesAt: string
  bookingOpen: boolean
}

export interface Announcement {
  id: number
  title: string
  body: string
  pinned: boolean
  author: string
  createdAt: string
}

export interface Bill {
  id: number
  month: string
  mealCount: number
  mealCharges: number
  fixedCharges: number
  total: number
  status: 'UNPAID' | 'PAID' | 'OVERDUE'
  dueDate: string
}

export interface Overview {
  approvedStudents: number
  pendingApprovals: number
  todayBookings: number
  todayServed: number
  revenueThisMonth: number
  outstanding: number
  overdueBills: number
  averageRating30d: number | null
  openComplaints: number
}

export interface LiveCounts {
  date: string
  mealType: MealType
  menuId: number | null
  booked: number
  served: number
  remaining: number
  noShow: number
  cancelled: number
}

/** RFC 7807 body returned by the API for every error. */
export interface ProblemDetail {
  status: number
  code?: string
  detail?: string
  errors?: { field: string; message: string }[]
}

export type BookingStatus = 'BOOKED' | 'CANCELLED' | 'CONSUMED' | 'NO_SHOW'
export interface Booking {
  id: number; dailyMenuId: number; date: string; mealType: MealType; status: BookingStatus; price: number; qrToken: string | null
  startTime: string; endTime: string; bookingClosesAt: string; cancellable: boolean; dishes: string[]
}
export interface QrInfo { bookingId: number; qrToken: string; payload: string; status: BookingStatus; date: string; mealType: MealType }
export interface BulkResult { booked: Booking[]; skipped: { date: string; mealType: MealType; code: string; reason: string }[] }
export interface PendingFeedback { dailyMenuId: number; date: string; mealType: MealType; dishes: string[] }
export interface FeedbackItem { id: number; dailyMenuId: number; date: string; mealType: MealType; rating: number; comment: string | null; createdAt: string }
export type ComplaintCategory = 'FOOD_QUALITY' | 'HYGIENE' | 'SERVICE' | 'BILLING' | 'OTHER'
export type ComplaintStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'REJECTED'
export interface ComplaintUpdateItem { id: number; authorName: string; authorRole: Role; message: string; newStatus: ComplaintStatus | null; createdAt: string }
export interface Complaint {
  id: number; category: ComplaintCategory; title: string; description: string; priority: 'LOW' | 'MEDIUM' | 'HIGH'; status: ComplaintStatus
  assignedToName: string | null; createdAt: string; updatedAt: string; updates: ComplaintUpdateItem[] | null
}
export interface BillLine { date: string; mealType: MealType; status: BookingStatus; price: number }
export interface BillDetail extends Bill { paidAt: string | null; lines: BillLine[] | null }
export interface Payment { id: number; billId: number; amount: number; method: 'UPI' | 'CARD' | 'CASH'; txnRef: string; status: 'PENDING' | 'SUCCESS' | 'FAILED'; paidAt: string | null; gatewayPayload: string | null; billMonth: string }

export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export interface StaffBooking { id: number; date: string; mealType: MealType; status: BookingStatus; price: number; studentName: string; rollNo: string | null }
export interface ScanResult { bookingId: number; studentName: string; rollNo: string | null; dietPref: DietPref | null; date: string; mealType: MealType; method: 'QR' | 'MANUAL'; markedAt: string }
export interface Student { userId: number; name: string; email: string; phone: string | null; rollNo: string; batchYear: number | null; hostelBlock: string | null; roomNo: string | null; dietPref: DietPref; approvalStatus: ApprovalStatus; active: boolean }
export interface AdminUser { id: number; name: string; email: string; role: Role; phone: string | null; active: boolean }
export interface MealSlot { mealType: MealType; startTime: string; endTime: string; cutoffMinutes: number; price: number; active: boolean }
export interface AdminBill extends Bill { studentName: string; rollNo: string | null }
export interface DayPoint { date: string; booked: number; served: number; noShow: number; cancelled: number }
export interface RevenuePoint { month: string; billed: number; collected: number; outstanding: number }
export interface DishScore { dishId: number; name: string; averageRating: number; ratings: number }
export interface FeedbackSummary { averageRating: number | null; totalRatings: number; byMeal: { mealType: MealType; average: number; count: number }[]; distribution: Record<string, number> }
export interface AdminFeedback { id: number; date: string; mealType: MealType; rating: number; comment: string | null; studentName: string | null }
export interface StaffComplaint extends Complaint { studentName: string; rollNo: string | null; assignedToId: number | null }
