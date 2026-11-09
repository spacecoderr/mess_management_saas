import { MutationCache, QueryCache, QueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { toApiError } from '@/lib/errors'

/** Failed reads surface as a toast (once); mutations show their own inline errors so they are not toasted here. */
export const queryClient = new QueryClient({
  queryCache: new QueryCache({
    onError: (error, query) => {
      const e = toApiError(error)
      if (e.status === 401 || query.meta?.silent) return   // 401 = session ended, the router already redirects
      toast.error(e.message, { id: String(query.queryHash) })
    },
  }),
  mutationCache: new MutationCache(),
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (count, error) => {
        const e = toApiError(error)
        return count < 1 && (e.isNetwork || e.status >= 500)   // never retry 4xx: they will fail the same way
      },
    },
  },
})
