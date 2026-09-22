import { Link, Route, Routes } from 'react-router-dom'
import { IdentitySwitcher } from './components/IdentitySwitcher'
import { useIdentity } from './context/IdentityContext'
import { DashboardPage } from './pages/DashboardPage'
import { OrderDetailPage } from './pages/OrderDetailPage'

function ClientBadge() {
  const { activeIdentity, isOperator, viewingClientId, identities } = useIdentity()

  if (!activeIdentity) return null

  if (isOperator) {
    const viewingClient = identities.find((i) => i.id === viewingClientId)
    return (
      <span className="rounded-full bg-indigo-100 px-3 py-1 text-sm text-indigo-800">
        Operator: {activeIdentity.displayName}
        {viewingClient
          ? ` — viewing ${viewingClient.displayName}${viewingClient.contractReference ? ` (Contract #${viewingClient.contractReference})` : ''}`
          : ''}
      </span>
    )
  }

  return (
    <span className="rounded-full bg-emerald-100 px-3 py-1 text-sm text-emerald-800">
      {activeIdentity.displayName}
      {activeIdentity.contractReference ? ` (Contract #${activeIdentity.contractReference})` : ''}
    </span>
  )
}

function Header() {
  return (
    <header className="flex flex-wrap items-center justify-between gap-4 border-b border-gray-200 px-6 py-4">
      <Link to="/" className="text-lg font-semibold text-gray-900">
        Bulk Hardware Order Management
      </Link>
      <div className="flex items-center gap-4">
        <ClientBadge />
        <IdentitySwitcher />
      </div>
    </header>
  )
}

function App() {
  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      <main className="mx-auto max-w-6xl px-6 py-8">
        <Routes>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/orders/new" element={<OrderDetailPage />} />
          <Route path="/orders/:orderId" element={<OrderDetailPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
