import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../api/client'
import type {
  LifecycleTransition,
  OrderDetail,
  OrderHistoryPage,
  OrderStatus,
  OrderSummary,
} from '../api/types'
import { useIdentity } from '../context/IdentityContext'

type FetchState = 'loading' | 'error' | 'loaded'

interface HistoryEntry {
  summary: OrderSummary
  transitions: LifecycleTransition[]
}

const STATUS_STYLES: Record<OrderStatus, string> = {
  INTAKE: 'bg-blue-100 text-blue-800',
  PROCESSING: 'bg-yellow-100 text-yellow-800',
  BACKORDERED: 'bg-amber-100 text-amber-800',
  SHIPPED: 'bg-purple-100 text-purple-800',
  FINAL_DELIVERY: 'bg-green-100 text-green-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
}

const dateFormat = new Intl.DateTimeFormat('en-US', { dateStyle: 'medium', timeStyle: 'short' })

async function loadEntriesForPage(headers: Record<string, string>, page: number) {
  const listPage = await apiFetch<OrderHistoryPage>(`/orders?page=${page}`, headers)
  const entries = await Promise.all(
    listPage.items.map(async (summary) => {
      const detail = await apiFetch<OrderDetail>(`/orders/${summary.id}`, headers)
      return { summary, transitions: detail.transitions }
    }),
  )
  return { entries, hasMore: listPage.hasMore }
}

/** Newest-first, paged Order History Log with full per-order transition timestamps (FR-008, FR-023). */
export function OrderHistoryLog() {
  const { headers } = useIdentity()
  const [state, setState] = useState<FetchState>('loading')
  const [entries, setEntries] = useState<HistoryEntry[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)

  function fetchFirstPage() {
    return loadEntriesForPage(headers, 0)
      .then((result) => {
        setEntries(result.entries)
        setHasMore(result.hasMore)
        setPage(0)
        setState('loaded')
      })
      .catch(() => setState('error'))
  }

  function retry() {
    setState('loading')
    fetchFirstPage()
  }

  const clientIdHeader = headers['X-Client-Id']

  useEffect(() => {
    if (!clientIdHeader) return
    fetchFirstPage()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientIdHeader])

  if (!clientIdHeader) {
    return <p className="text-gray-600">Select a client company above to view its order history.</p>
  }

  async function loadOlderEntries() {
    setLoadingMore(true)
    try {
      const nextPage = page + 1
      const result = await loadEntriesForPage(headers, nextPage)
      setEntries((prev) => [...prev, ...result.entries])
      setHasMore(result.hasMore)
      setPage(nextPage)
    } catch {
      setState('error')
    } finally {
      setLoadingMore(false)
    }
  }

  if (state === 'loading') {
    return <p className="text-gray-500">Loading order history…</p>
  }

  if (state === 'error') {
    return (
      <div className="rounded border border-red-200 bg-red-50 p-4">
        <p className="text-red-600">Unable to load order history.</p>
        <button
          type="button"
          onClick={retry}
          className="mt-2 text-sm font-medium text-red-700 underline"
        >
          Retry
        </button>
      </div>
    )
  }

  if (entries.length === 0) {
    return (
      <div className="rounded border border-gray-200 p-6 text-center">
        <p className="text-gray-600">No past orders.</p>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {entries.map(({ summary, transitions }) => (
        <div key={summary.id} className="rounded-lg border border-gray-200 p-4">
          <div className="flex items-center justify-between gap-4">
            <Link
              to={`/orders/${summary.id}`}
              className="font-medium text-indigo-700 hover:underline"
            >
              Order {summary.id.slice(0, 8)}
            </Link>
            <span
              className={`rounded-full px-2 py-1 text-xs font-medium ${STATUS_STYLES[summary.status]}`}
            >
              {summary.status.replace('_', ' ')}
            </span>
          </div>
          <ul className="mt-2 space-y-1 text-xs text-gray-500">
            {transitions.map((transition, index) => (
              <li key={index}>
                {(transition.fromStatus ? `${transition.fromStatus} → ` : '') + transition.toStatus}{' '}
                — {dateFormat.format(new Date(transition.occurredAt))}
              </li>
            ))}
          </ul>
        </div>
      ))}
      {hasMore && (
        <button
          type="button"
          onClick={loadOlderEntries}
          disabled={loadingMore}
          className="w-full rounded border border-gray-300 py-2 text-sm text-gray-700 disabled:opacity-50"
        >
          {loadingMore ? 'Loading…' : 'Reach older entries'}
        </button>
      )}
    </div>
  )
}
