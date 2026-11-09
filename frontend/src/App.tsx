import { QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { Toaster } from 'sonner'
import { useThemeSync } from '@/components/ui/ThemeToggle'
import { queryClient } from '@/lib/queryClient'
import { AppRoutes } from './routes'

export default function App() {
  useThemeSync()
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppRoutes />
      </BrowserRouter>
      <Toaster position="top-right" richColors closeButton toastOptions={{ style: { backdropFilter: 'blur(12px)' } }} />
    </QueryClientProvider>
  )
}
