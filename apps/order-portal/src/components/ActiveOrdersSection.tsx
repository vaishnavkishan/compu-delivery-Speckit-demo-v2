import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { apiFetch, ApiError } from '../api/client'
import type { OrderStatus, OrderSummary } from '../api/types'
import { useIdentity } from '../context/IdentityContext'
import { LifecycleStepper } from './LifecycleStepper'

type FetchState = 'loading' | 'error' | 'loaded'

const ACTIVE_STATUSES: OrderStatus[] = ['INTAKE', 'PROCESSING', 'BACKORDERED', 'SHIPPED']

const NEXT_FORWARD_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
  INTAKE: 'PROCESSING',
  PROCESSING: 'SHIPPED',
  SHIPPED: 'FINAL_DELIVERY',
}

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

/**
 * One card per Active order with its lifecycle stepper, a client cancel
 * control (explicit confirmation), and operator-only lifecycle controls
 * (FR-022, FR-026, FR-028, FR-029). Fetches and tracks its own request state
 * independently of `OrderHistoryLog`.
 */
export function ActiveOrdersSection() {
  const { headers, isClient, isOperator } = useIdentity()
  const navigate = useNavigate()
  const [state, setState] = useState<FetchState>('loading')
  const [orders, setOrders] = useState<OrderSummary[]>([])
  const [confirmingCancelId, setConfirmingCancelId] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [busyOrderId, setBusyOrderId] = useState<string | null>(null)

  function fetchOrders() {
    return apiFetch<{ items: OrderSummary[] }>('/orders?page=0', headers)
      .then((data) => {
        setOrders(data.items)
        setState('loaded')
      })
      .catch(() => setState('error'))
  }

  function retry() {
    setState('loading')
    fetchOrders()
  }

  const clientIdHeader = headers['X-Client-Id']

  useEffect(() => {
    if (!clientIdHeader) return
    fetchOrders()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clientIdHeader])

  if (!clientIdHeader) {
    return <p className="text-gray-600">Select a client company above to view its orders.</p>
  }

  async function handleCancel(orderId: string) {
    setBusyOrderId(orderId)
    setActionError(null)
    try {
      await apiFetch(`/orders/${orderId}/cancel`, headers, { method: 'POST' })
      setOrders((prev) => prev.filter((o) => o.id !== orderId))
      setConfirmingCancelId(null)
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Unable to cancel this order.')
    } finally {
      setBusyOrderId(null)
    }
  }

  async function handleAdvance(orderId: string, targetStatus: OrderStatus) {
    setBusyOrderId(orderId)
    setActionError(null)
    try {
      const updated = await apiFetch<{
        status: OrderStatus
        netTotal: number
        createdAt: string
        updatedAt: string
      }>(`/orders/${orderId}/status`, headers, {
        method: 'POST',
        body: JSON.stringify({ targetStatus }),
      })
      setOrders((prev) =>
        prev.map((o) =>
          o.id === orderId ? { ...o, status: updated.status, updatedAt: updated.updatedAt } : o,
        ),
      )
    } catch (error) {
      setActionError(error instanceof ApiError ? error.message : 'Unable to advance this order.')
    } finally {
      setBusyOrderId(null)
    }
  }

  if (state === 'loading') {
    return <p className="text-gray-500">Loading active orders…</p>
  }

  if (state === 'error') {
    return (
      <div className="rounded border border-red-200 bg-red-50 p-4">
        <p className="text-red-600">Unable to load active orders.</p>
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

  const activeOrders = orders.filter((o) => ACTIVE_STATUSES.includes(o.status))

  if (activeOrders.length === 0) {
    return (
      <div className="rounded border border-gray-200 p-6 text-center">
        <p className="text-gray-600">No active orders.</p>
        {isClient && (
          <button
            type="button"
            onClick={() => navigate('/orders/new')}
            className="mt-3 rounded bg-indigo-600 px-4 py-2 text-sm font-medium text-white"
          >
            Start a new order
          </button>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {actionError && <p className="text-sm text-red-600">{actionError}</p>}
      {activeOrders.map((order) => {
        const nextStatus = NEXT_FORWARD_STATUS[order.status]
        const isBusy = busyOrderId === order.id
        return (
          <div key={order.id} className="rounded-lg border border-gray-200 p-4">
            <div className="flex items-center justify-between gap-4">
              <Link
                to={`/orders/${order.id}`}
                className="font-medium text-indigo-700 hover:underline"
              >
                Order {order.id.slice(0, 8)}
              </Link>
              <span className="text-sm text-gray-600">{currency.format(order.netTotal)}</span>
            </div>
            <div className="mt-3">
              <LifecycleStepper status={order.status} />
            </div>
            <div className="mt-4 flex flex-wrap items-center gap-2">
              {isClient && confirmingCancelId !== order.id && (
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => setConfirmingCancelId(order.id)}
                  className="rounded border border-red-300 px-3 py-1 text-sm text-red-700 disabled:opacity-50"
                >
                  Cancel order
                </button>
              )}
              {isClient && confirmingCancelId === order.id && (
                <span
                  role="alertdialog"
                  aria-live="assertive"
                  aria-label={`Confirm cancellation of order ${order.id.slice(0, 8)}`}
                  className="flex items-center gap-2 text-sm"
                >
                  <span className="text-gray-700">Cancel this order?</span>
                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={() => handleCancel(order.id)}
                    className="rounded bg-red-600 px-3 py-1 text-white disabled:opacity-50"
                  >
                    {isBusy ? 'Cancelling…' : 'Confirm cancel'}
                  </button>
                  <button
                    type="button"
                    disabled={isBusy}
                    onClick={() => setConfirmingCancelId(null)}
                    className="rounded border border-gray-300 px-3 py-1 text-gray-700"
                  >
                    Keep order
                  </button>
                </span>
              )}
              {isOperator && nextStatus && (
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => handleAdvance(order.id, nextStatus)}
                  className="rounded bg-indigo-600 px-3 py-1 text-sm text-white disabled:opacity-50"
                >
                  Advance to {nextStatus.replace('_', ' ')}
                </button>
              )}
              {isOperator && order.status === 'PROCESSING' && (
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => handleAdvance(order.id, 'BACKORDERED')}
                  className="rounded border border-amber-300 px-3 py-1 text-sm text-amber-700 disabled:opacity-50"
                >
                  Mark backordered
                </button>
              )}
              {isOperator && order.status === 'BACKORDERED' && (
                <button
                  type="button"
                  disabled={isBusy}
                  onClick={() => handleAdvance(order.id, 'PROCESSING')}
                  className="rounded border border-amber-300 px-3 py-1 text-sm text-amber-700 disabled:opacity-50"
                >
                  Return to processing
                </button>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}
