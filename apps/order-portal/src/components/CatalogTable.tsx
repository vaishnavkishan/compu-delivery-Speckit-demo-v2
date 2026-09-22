import type { HardwareCatalogItem } from '../api/types'

const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

interface CatalogTableProps {
  catalog: HardwareCatalogItem[]
  quantities: Record<string, number>
  onQuantityChange: (sku: string, quantity: number) => void
}

/** Fixed Hardware Catalog with per-item quantity entry and a live per-line subtotal (FR-020). */
export function CatalogTable({ catalog, quantities, onQuantityChange }: CatalogTableProps) {
  return (
    <table className="w-full border-collapse text-sm">
      <caption className="sr-only">Hardware catalog</caption>
      <thead>
        <tr className="border-b border-gray-200 text-left text-gray-500">
          <th scope="col" className="py-2 pr-4 font-medium">
            SKU
          </th>
          <th scope="col" className="py-2 pr-4 font-medium">
            Item
          </th>
          <th scope="col" className="py-2 pr-4 font-medium">
            List price
          </th>
          <th scope="col" className="py-2 pr-4 font-medium">
            Quantity
          </th>
          <th scope="col" className="py-2 pr-4 font-medium text-right">
            Subtotal
          </th>
        </tr>
      </thead>
      <tbody>
        {catalog.map((item) => {
          const quantity = quantities[item.sku] ?? 0
          const subtotal = quantity * item.listPrice
          return (
            <tr key={item.sku} className="border-b border-gray-100">
              <td className="py-2 pr-4 text-gray-500">{item.sku}</td>
              <td className="py-2 pr-4">{item.name}</td>
              <td className="py-2 pr-4">{currency.format(item.listPrice)}</td>
              <td className="py-2 pr-4">
                <label className="sr-only" htmlFor={`quantity-${item.sku}`}>
                  Quantity for {item.name}
                </label>
                <input
                  id={`quantity-${item.sku}`}
                  type="number"
                  min={0}
                  max={10000}
                  value={quantity}
                  onChange={(e) => onQuantityChange(item.sku, Math.max(0, Number(e.target.value) || 0))}
                  className="w-24 rounded border border-gray-300 px-2 py-1"
                />
              </td>
              <td className="py-2 pr-4 text-right">{currency.format(subtotal)}</td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
