import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { PricingSummary } from '../src/components/PricingSummary'

describe('PricingSummary', () => {
  it('shows placeholders for discount and net total before the server has priced the order', () => {
    render(<PricingSummary grossSubtotal={1000} />)

    expect(screen.getByText('$1,000.00')).toBeInTheDocument()
    expect(screen.getAllByText('—')).toHaveLength(2)
  })

  it('recomputes the gross subtotal, discount, and net total together as quantities change', () => {
    const { rerender } = render(
      <PricingSummary grossSubtotal={2000} appliedDiscountPercentage={10} />,
    )

    expect(screen.getByText('$2,000.00')).toBeInTheDocument()
    expect(screen.getByText('Contract Discount (10%)')).toBeInTheDocument()
    expect(screen.getByText('-$200.00')).toBeInTheDocument()
    expect(screen.getByText('$1,800.00')).toBeInTheDocument()

    rerender(<PricingSummary grossSubtotal={3000} appliedDiscountPercentage={10} />)

    expect(screen.getByText('$3,000.00')).toBeInTheDocument()
    expect(screen.getByText('-$300.00')).toBeInTheDocument()
    expect(screen.getByText('$2,700.00')).toBeInTheDocument()
  })

  it('always shows Freight & Logistics as Waived', () => {
    render(<PricingSummary grossSubtotal={0} />)

    expect(screen.getByText('Waived')).toBeInTheDocument()
  })
})
