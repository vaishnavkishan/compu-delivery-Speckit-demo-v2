import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { apiFetch } from '../api/client'
import type { DemoIdentity } from '../api/types'

interface IdentityContextValue {
  identities: DemoIdentity[]
  loading: boolean
  error: string | null
  reload: () => void
  activeIdentity: DemoIdentity | null
  setActiveIdentity: (identity: DemoIdentity | null) => void
  /** When activeIdentity is an operator, the client company currently selected to view/act on (FR-026). */
  viewingClientId: string | null
  setViewingClientId: (clientId: string | null) => void
  /** X-Client-Id / X-Operator-Id headers to attach to every API call for the current selection. */
  headers: Record<string, string>
  isOperator: boolean
  isClient: boolean
}

const IdentityContext = createContext<IdentityContextValue | undefined>(undefined)

export function IdentityProvider({ children }: { children: ReactNode }) {
  const [identities, setIdentities] = useState<DemoIdentity[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [activeIdentity, setActiveIdentity] = useState<DemoIdentity | null>(null)
  const [viewingClientId, setViewingClientId] = useState<string | null>(null)
  const [reloadToken, setReloadToken] = useState(0)

  useEffect(() => {
    let cancelled = false
    apiFetch<DemoIdentity[]>('/demo-identities', {})
      .then((data) => {
        if (!cancelled) setIdentities(data)
      })
      .catch(() => {
        if (!cancelled) setError('Unable to load demo identities')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [reloadToken])

  const isOperator = activeIdentity?.role === 'OPERATOR'
  const isClient = activeIdentity?.role === 'CLIENT'

  const headers = useMemo(() => {
    if (!activeIdentity) return {}
    if (isClient) return { 'X-Client-Id': activeIdentity.id }
    if (isOperator) {
      const h: Record<string, string> = { 'X-Operator-Id': activeIdentity.id }
      if (viewingClientId) h['X-Client-Id'] = viewingClientId
      return h
    }
    return {}
  }, [activeIdentity, isClient, isOperator, viewingClientId])

  function handleSetActiveIdentity(identity: DemoIdentity | null) {
    setActiveIdentity(identity)
    setViewingClientId(identity?.role === 'OPERATOR' ? viewingClientId : null)
  }

  const value: IdentityContextValue = {
    identities,
    loading,
    error,
    reload: () => {
      setLoading(true)
      setError(null)
      setReloadToken((t) => t + 1)
    },
    activeIdentity,
    setActiveIdentity: handleSetActiveIdentity,
    viewingClientId,
    setViewingClientId,
    headers,
    isOperator,
    isClient,
  }

  return <IdentityContext.Provider value={value}>{children}</IdentityContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components -- context hook lives alongside its provider by design
export function useIdentity(): IdentityContextValue {
  const context = useContext(IdentityContext)
  if (!context) {
    throw new Error('useIdentity must be used within an IdentityProvider')
  }
  return context
}
