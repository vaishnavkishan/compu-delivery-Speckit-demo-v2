import { useNavigate } from 'react-router-dom'
import { ActiveOrdersSection } from '../components/ActiveOrdersSection'
import { OrderHistoryLog } from '../components/OrderHistoryLog'
import { useIdentity } from '../context/IdentityContext'

/** Active Orders & Lifecycle Tracking plus the Order History Log (FR-008, FR-022, FR-028, FR-029). */
export function DashboardPage() {
  const navigate = useNavigate()
  const { activeIdentity, isClient } = useIdentity()

  if (!activeIdentity) {
    return <p className="text-gray-600">Select a demo identity above to view its orders.</p>
  }

  return (
    <div className="space-y-10">
      <section>
        <div className="mb-4 flex items-center justify-between">
          <h1 className="text-xl font-semibold text-gray-900">
            Active Orders &amp; Lifecycle Tracking
          </h1>
          {isClient && (
            <button
              type="button"
              onClick={() => navigate('/orders/new')}
              className="rounded bg-indigo-600 px-4 py-2 text-sm font-medium text-white"
            >
              Start a new order
            </button>
          )}
        </div>
        <ActiveOrdersSection />
      </section>
      <section>
        <h2 className="mb-4 text-xl font-semibold text-gray-900">Order History</h2>
        <OrderHistoryLog />
      </section>
    </div>
  )
}
