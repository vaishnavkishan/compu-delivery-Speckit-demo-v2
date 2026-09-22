import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { CatalogTable } from '../src/components/CatalogTable'

const catalog = [
  { sku: 'SKU-1001', name: 'Rack Server', listPrice: 4200 },
  { sku: 'SKU-1002', name: 'Network Switch', listPrice: 800 },
]

describe('CatalogTable', () => {
  it('renders a live per-line subtotal for each catalog item', () => {
    render(
      <CatalogTable catalog={catalog} quantities={{ 'SKU-1001': 2 }} onQuantityChange={vi.fn()} />,
    )

    expect(screen.getByText('$8,400.00')).toBeInTheDocument()
    expect(screen.getByText('$0.00')).toBeInTheDocument()
  })

  it('calls onQuantityChange with the new quantity as the user types', async () => {
    const onQuantityChange = vi.fn()
    render(<CatalogTable catalog={catalog} quantities={{}} onQuantityChange={onQuantityChange} />)

    const input = screen.getByLabelText('Quantity for Rack Server')
    await userEvent.clear(input)
    await userEvent.type(input, '5')

    expect(onQuantityChange).toHaveBeenLastCalledWith('SKU-1001', 5)
  })
})
