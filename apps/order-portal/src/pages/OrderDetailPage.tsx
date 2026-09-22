import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { apiFetch, ApiError } from '../api/client'
import type { HardwareCatalogItem, LineItemInput, OrderDetail } from '../api/types'
import { CatalogTable } from '../components/CatalogTable'
import { LifecycleStepper } from '../components/LifecycleStepper'
import { PricingSummary } from '../components/PricingSummary'
import { useIdentity } from '../context/IdentityContext'

type FetchState = 'idle' | 'loading' | 'error' | 'loaded'

/**
 * Bulk Order Catalog + pricing summary for creating a new order, or editing an existing one
 * while it is Intake/Processing (FR-020, FR-021, FR-024, FR-025).
 */
export function OrderDetailPage() {
  const { orderId } = useParams<{ orderId: string }>()
  const navigate = useNavigate()
  const { headers, isClient } = useIdentity()
  const isEditing = Boolean(orderId)

  const [catalog, setCatalog] = useState<HardwareCatalogItem[]>([])
  const [catalogState, setCatalogState] = useState<FetchState>('loading')
  const [quantities, setQuantities] = useState<Record<string, number>>({})
  const [pricedOrder, setPricedOrder] = useState<OrderDetail | undefined>(undefined)
  const [orderState, setOrderState] = useState<FetchState>(isEditing ? 'loading' : 'loaded')
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let cancelled = false
    apiFetch<HardwareCatalogItem[]>('/catalog', {})
      .then((data) => {
        if (!cancelled) {
          setCatalog(data)
          setCatalogState('loaded')
        }
      })
      .catch(() => {
        if (!cancelled) setCatalogState('error')
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    if (!orderId || pricedOrder?.id === orderId) return
    let cancelled = false
    apiFetch<OrderDetail>(`/orders/${orderId}`, headers)
      .then((data) => {
        if (cancelled) return
        setPricedOrder(data)
        const initialQuantities: Record<string, number> = {}
        for (const item of data.lineItems) {
          initialQuantities[item.sku] = (initialQuantities[item.sku] ?? 0) + item.quantity
        }
        setQuantities(initialQuantities)
        setOrderState('loaded')
      })
      .catch(() => {
        if (!cancelled) setOrderState('error')
      })
    return () => {
      cancelled = true
    }
    // headers intentionally excluded: refetch is driven by orderId, not by identity switching mid-edit
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderId])

  const grossSubtotal = catalog.reduce(
    (sum, item) => sum + (quantities[item.sku] ?? 0) * item.listPrice,
    0,
  )
  const totalQuantity = Object.values(quantities).reduce((sum, q) => sum + q, 0)

  function handleQuantityChange(sku: string, quantity: number) {
    setQuantities((prev) => ({ ...prev, [sku]: quantity }))
  }

  async function handleSubmit() {
    if (totalQuantity === 0) {
      setSubmitError('Add at least one item before submitting the order.')
      return
    }
    const lineItems: LineItemInput[] = Object.entries(quantities)
      .filter(([, quantity]) => quantity > 0)
      .map(([sku, quantity]) => ({ sku, quantity }))

    setSubmitting(true)
    setSubmitError(null)
    try {
      const result = isEditing
        ? await apiFetch<OrderDetail>(`/orders/${orderId}/line-items`, headers, {
            method: 'PUT',
            body: JSON.stringify({ lineItems }),
          })
        : await apiFetch<OrderDetail>('/orders', headers, {
            method: 'POST',
            body: JSON.stringify({ lineItems }),
          })
      setPricedOrder(result)
      if (!isEditing) {
        navigate(`/orders/${result.id}`)
      }
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.message : 'Unable to submit the order.')
    } finally {
      setSubmitting(false)
    }
  }

  if (!isClient) {
    return <p className="text-gray-600">Select a client identity to create or edit an order.</p>
  }

  return (
    <div className="grid gap-8 lg:grid-cols-[2fr_1fr]">
      <section>
        <h1 className="mb-4 text-xl font-semibold text-gray-900">
          {isEditing ? 'Edit Bulk Order' : 'New Bulk Order'}
        </h1>
        {isEditing && pricedOrder && (
          <div className="mb-6">
            <LifecycleStepper status={pricedOrder.status} />
          </div>
        )}
        {orderState === 'loading' && <p className="text-gray-500">Loading order…</p>}
        {orderState === 'error' && <p className="text-red-600">Unable to load this order.</p>}
        {catalogState === 'loading' && <p className="text-gray-500">Loading catalog…</p>}
        {catalogState === 'error' && (
          <p className="text-red-600">Unable to load the hardware catalog.</p>
        )}
        {catalogState === 'loaded' && orderState !== 'loading' && orderState !== 'error' && (
          <CatalogTable
            catalog={catalog}
            quantities={quantities}
            onQuantityChange={handleQuantityChange}
          />
        )}
      </section>
      <aside className="rounded-lg border border-gray-200 p-4">
        <h2 className="mb-2 text-lg font-semibold text-gray-900">Pricing Summary</h2>
        <PricingSummary
          grossSubtotal={grossSubtotal}
          appliedDiscountPercentage={pricedOrder?.appliedDiscountPercentage}
          netTotal={pricedOrder?.netTotal}
        />
        {submitError && <p className="mt-2 text-sm text-red-600">{submitError}</p>}
        <button
          type="button"
          onClick={handleSubmit}
          disabled={submitting || totalQuantity === 0}
          className="mt-4 w-full rounded bg-indigo-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-50"
        >
          {submitting ? 'Submitting…' : isEditing ? 'Save Changes' : 'Submit Order'}
        </button>
      </aside>
    </div>
  )
}
