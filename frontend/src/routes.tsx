import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'
import { AppShell } from '@/components/layout/AppShell'
import { ProtectedRoute, PublicOnlyRoute, RoleRedirect } from '@/components/guards/guards'
import { PageLoader } from '@/components/ui/Skeleton'
import { ForbiddenPage, NotFoundPage } from '@/features/common/ErrorPages'
import LandingPage from '@/features/landing/LandingPage'

// Pages are code-split: a student never downloads the admin dashboard.
const LoginPage = lazy(() => import('@/features/auth/LoginPage'))
const RegisterPage = lazy(() => import('@/features/auth/RegisterPage'))
const ForgotPasswordPage = lazy(() => import('@/features/auth/ForgotPasswordPage'))
const PendingApprovalPage = lazy(() => import('@/features/auth/PendingApprovalPage'))
const StudentHome = lazy(() => import('@/features/student/StudentHome'))
const ServeHome = lazy(() => import('@/features/staff/ServeHome'))
const AdminDashboard = lazy(() => import('@/features/admin/AdminDashboard'))
const MenuPage = lazy(() => import('@/features/student/MenuPage'))
const BookingsPage = lazy(() => import('@/features/student/BookingsPage'))
const BillsPage = lazy(() => import('@/features/student/BillsPage'))
const FeedbackPage = lazy(() => import('@/features/student/FeedbackPage'))
const ComplaintsPage = lazy(() => import('@/features/student/ComplaintsPage'))
const ProfilePage = lazy(() => import('@/features/student/ProfilePage'))
const TodayMenuPage = lazy(() => import('@/features/staff/TodayMenuPage'))
const ComplaintsQueue = lazy(() => import('@/features/common/ComplaintsQueue'))
const StudentsPage = lazy(() => import('@/features/admin/StudentsPage'))
const BillingPage = lazy(() => import('@/features/admin/BillingPage'))
const MenuBuilderPage = lazy(() => import('@/features/admin/MenuBuilderPage'))
const DishesPage = lazy(() => import('@/features/admin/DishesPage'))
const MealSettingsPage = lazy(() => import('@/features/admin/MealSettingsPage'))
const AttendancePage = lazy(() => import('@/features/admin/AttendancePage'))
const StaffPage = lazy(() => import('@/features/admin/StaffPage'))
const AnnouncementsPage = lazy(() => import('@/features/admin/AnnouncementsPage'))
const FeedbackAdminPage = lazy(() => import('@/features/admin/FeedbackAdminPage'))


/** Pages shared by several roles: students get the real screen now, staff/admin versions arrive in Phases 7-8. */
function RoleSwitch({ student, staff }: { student: JSX.Element; staff: JSX.Element }) {
  const role = useAuthStore((s) => s.user?.role)
  return role === 'STUDENT' ? student : staff
}
function FeedbackSwitch() {
  const role = useAuthStore((s) => s.user?.role)
  return role === 'STUDENT' ? <FeedbackPage /> : <FeedbackAdminPage />
}

export function AppRoutes() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<LandingPage />} />
        <Route element={<PublicOnlyRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        </Route>
        <Route path="/pending-approval" element={<PendingApprovalPage />} />

        <Route path="/app" element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route index element={<RoleRedirect />} />

            <Route element={<ProtectedRoute roles={['STUDENT']} />}>
              <Route path="home" element={<StudentHome />} />
              <Route path="menu" element={<MenuPage />} />
              <Route path="bookings" element={<BookingsPage />} />
              <Route path="bills" element={<BillsPage />} />
            </Route>

            <Route element={<ProtectedRoute roles={['STAFF']} />}>
              <Route path="serve" element={<ServeHome />} />
              <Route path="menu-today" element={<TodayMenuPage />} />
            </Route>

            <Route element={<ProtectedRoute roles={['ADMIN']} />}>
              <Route path="dashboard" element={<AdminDashboard />} />
                            <Route path="students" element={<StudentsPage />} />
              <Route path="billing" element={<BillingPage />} />
              <Route path="menu-builder" element={<MenuBuilderPage />} />
              <Route path="dishes" element={<DishesPage />} />
              <Route path="meal-settings" element={<MealSettingsPage />} />
              <Route path="attendance" element={<AttendancePage />} />
              <Route path="staff" element={<StaffPage />} />
              <Route path="announcements" element={<AnnouncementsPage />} />
            </Route>

            <Route element={<ProtectedRoute roles={['STUDENT', 'ADMIN']} />}>
              <Route path="feedback" element={<FeedbackSwitch />} />
            </Route>
            <Route path="complaints" element={<RoleSwitch student={<ComplaintsPage />} staff={<ComplaintsQueue />} />} />
            <Route path="profile" element={<ProfilePage />} />
          </Route>
        </Route>

        <Route path="/403" element={<ForbiddenPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  )
}
