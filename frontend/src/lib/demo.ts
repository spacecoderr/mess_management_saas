/** Demo accounts created by the backend seeder. Shown on the login page in dev, or when VITE_SHOW_DEMO=true. */
export const SHOW_DEMO = import.meta.env.DEV || import.meta.env.VITE_SHOW_DEMO === 'true'

export const DEMO_ACCOUNTS = [
  { role: 'Admin', email: 'admin@thali.local', password: 'Admin@12345' },
  { role: 'Staff', email: 'staff1@thali.local', password: 'Demo@1234' },
  { role: 'Student', email: 'z900002@iiit-bh.ac.in', password: 'Demo@1234' },
] as const
