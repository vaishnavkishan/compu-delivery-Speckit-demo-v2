import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { OrderHistoryLog } from '../src/components/OrderHistoryLog'
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
      <OrderHistoryLog />
    </MemoryRouter>,
  )
}

describe('OrderHistoryLog', () => {
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

  it('shows a loading indicator while order history is being fetched', () => {
    vi.mocked(apiFetch).mockReturnValue(new Promise(() => {}))

    renderWithRouter()

    expect(screen.getByText(/loading order history/i)).toBeInTheDocument()
  })

  it('shows the "No past orders" empty state once loaded with none', async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      items: [],
      page: 0,
      pageSize: 25,
      totalCount: 0,
      hasMore: false,
    })

    renderWithRouter()

    await waitFor(() => expect(screen.getByText('No past orders.')).toBeInTheDocument())
  })

  it('shows a plain error message with a retry action when retrieval fails', async () => {
    vi.mocked(apiFetch).mockRejectedValue(new Error('network down'))

    renderWithRouter()

    await waitFor(() =>
      expect(screen.getByText(/unable to load order history/i)).toBeInTheDocument(),
    )
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument()
  })

  it('renders each entry with its status and full transition history, and a "reach older entries" control when more pages remain', async () => {
    vi.mocked(apiFetch).mockResolvedValue({
      items: [
        {
          id: 'order-1',
          status: 'CANCELLED',
          lineItems: [],
          grossTotal: 100,
          netTotal: 100,
          createdAt: '',
          updatedAt: '',
          transitions: [
            {
              fromStatus: null,
              toStatus: 'INTAKE',
              actorType: 'CLIENT',
              actorId: 'ACME-001',
              occurredAt: '2026-01-01T00:00:00Z',
            },
            {
              fromStatus: 'INTAKE',
              toStatus: 'CANCELLED',
              actorType: 'CLIENT',
              actorId: 'ACME-001',
              occurredAt: '2026-01-02T00:00:00Z',
            },
          ],
        },
      ],
      page: 0,
      pageSize: 25,
      totalCount: 26,
      hasMore: true,
    })

    renderWithRouter()

    await waitFor(() => expect(screen.getByText('CANCELLED')).toBeInTheDocument())
    expect(screen.getByText(/INTAKE → CANCELLED/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reach older entries/i })).toBeInTheDocument()
  })
})
