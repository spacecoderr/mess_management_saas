import { Suspense, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { MeshBackground } from '@/components/ui/MeshBackground'
import { PageLoader } from '@/components/ui/Skeleton'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'
import { BottomNav, Drawer } from './MobileNav'

/** Signed-in layout: glass sidebar on desktop, bottom bar + drawer on phones. Pages render into <Outlet />. */
export function AppShell() {
  const [drawer, setDrawer] = useState(false)
  const { pathname } = useLocation()
  useEffect(() => setDrawer(false), [pathname])

  return (
    <>
      <MeshBackground />
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-full focus:bg-primary focus:px-4 focus:py-2 focus:text-on-primary">
        Skip to content
      </a>
      <div className="mx-auto flex min-h-screen max-w-[1500px] gap-6 p-3 lg:p-6">
        <aside className="hidden w-64 shrink-0 lg:block">
          <Sidebar />
        </aside>
        <div className="flex min-w-0 flex-1 flex-col">
          <Topbar />
          <main id="main" className="flex-1 pb-28 lg:pb-8">
            <Suspense fallback={<PageLoader />}>
              <Outlet />
            </Suspense>
          </main>
        </div>
      </div>
      <BottomNav onMore={() => setDrawer(true)} />
      <Drawer open={drawer} onClose={() => setDrawer(false)} />
    </>
  )
}
