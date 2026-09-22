import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ActiveOrdersSection } from '../src/components/ActiveOrdersSection'
import { apiFetch } from '../src/api/client'
import { useIdentity } from '../src/context/IdentityContext'

vi.mock('../src/api/client', () => ({
  apiFetch: vi.fn(),
  ApiError: class ApiError extends Error {},
}))

vi.mock('../src/context/IdentityContext', () => ({
  useIdentity: vi.fn(),
}))

function renderWithRouter() {
  return render(
    <MemoryRouter>
      <ActiveOrdersSection />
    </MemoryRouter>,
  )
}

describe('ActiveOrdersSection', () => {
  beforeEach(() => {
    vi.mocked(useIdentity).mockReturnValue({
      headers: { 'X-Client-Id': 'ACME-001' },
      isClient: true,
      isOperator: false,
    } as ReturnType<typeof useIdentity>)
  })

  afterEach(() => {
    vi.mocked(apiFetch).mockReset()
  })

  it('shows a loading indicator while the active orders are being fetched', () => {
    vi.mocked(apiFetch).mockReturnValue(new Promise(() => {}))

    renderWithRouter()

    expect(screen.getByText(/loading active orders/i)).toBeInTheDocument()
  })

  it('shows the "No active orders" empty state with a start-order CTA once loaded with none', async () => {
    vi.mocked(apiFetch).mockResolvedValue({ items: [] })

    renderWithRouter()

    await waitFor(() => expect(screen.getByText('No active orders.')).toBeInTheDocument())
    expect(screen.getByRole('button', { name: /start a new order/i })).toBeInTheDocument()
  })

  it('shows a plain error message with a retry action when retrieval fails', async () => {
    vi.mocked(apiFetch).mockRejectedValue(new Error('network down'))

    renderWithRouter()

    await waitFor(() =>
      expect(screen.getByText(/unable to load active orders/i)).toBeInTheDocument(),
    )
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })

  it('renders a card with a lifecycle stepper for each active order', async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      items: [{ id: 'order-1', status: 'PROCESSING', netTotal: 100, createdAt: '', updatedAt: '' }],
    })

    renderWithRouter()

    await waitFor(() => expect(screen.getByText(/order-1/i)).toBeInTheDocument())
    expect(screen.getByRole('list', { name: /order lifecycle progress/i })).toBeInTheDocument()
  })
})
