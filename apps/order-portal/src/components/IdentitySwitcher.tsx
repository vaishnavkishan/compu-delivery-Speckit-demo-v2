import { useIdentity } from '../context/IdentityContext'

/** Lets the user pick the active demo client/operator identity in place of a login flow (FR-019). */
export function IdentitySwitcher() {
  const { identities, loading, error, activeIdentity, setActiveIdentity, viewingClientId, setViewingClientId, isOperator } =
    useIdentity()

  if (loading) {
    return <span className="text-sm text-gray-500">Loading identities…</span>
  }
  if (error) {
    return <span className="text-sm text-red-600">{error}</span>
  }

  const clientIdentities = identities.filter((i) => i.role === 'CLIENT')

  return (
    <div className="flex items-center gap-3">
      <label className="flex items-center gap-2 text-sm">
        <span className="text-gray-600">Viewing as</span>
        <select
          aria-label="Select demo identity"
          className="rounded border border-gray-300 px-2 py-1 text-sm"
          value={activeIdentity?.id ?? ''}
          onChange={(e) => {
            const found = identities.find((i) => i.id === e.target.value) ?? null
            setActiveIdentity(found)
          }}
        >
          <option value="" disabled>
            Select an identity…
          </option>
          {identities.map((identity) => (
            <option key={identity.id} value={identity.id}>
              {identity.displayName} ({identity.role === 'CLIENT' ? 'Client' : 'Operator'})
            </option>
          ))}
        </select>
      </label>

      {isOperator && (
        <label className="flex items-center gap-2 text-sm">
          <span className="text-gray-600">Client company</span>
          <select
            aria-label="Select client company to view"
            className="rounded border border-gray-300 px-2 py-1 text-sm"
            value={viewingClientId ?? ''}
            onChange={(e) => setViewingClientId(e.target.value || null)}
          >
            <option value="">Select a client…</option>
            {clientIdentities.map((client) => (
              <option key={client.id} value={client.id}>
                {client.displayName}
              </option>
            ))}
          </select>
        </label>
      )}
    </div>
  )
}
