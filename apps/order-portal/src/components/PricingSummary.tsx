const currency = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

interface PricingSummaryProps {
  grossSubtotal: number
  /** Known only once the server has priced the order (create/edit response); undefined before then. */
  appliedDiscountPercentage?: number
  netTotal?: number
}

/**
 * Gross Subtotal, Contract Discount, Freight & Logistics (always informational "Waived"), and
 * Final Net Total, in that order (FR-021). Gross Subtotal recomputes live as catalog quantities
 * change; the discount/net figures reflect the server's most recent pricing response, since Net
 * Total derives exclusively from the client's contract terms (FR-003) rather than a client-side
 * estimate.
 */
export function PricingSummary({ grossSubtotal, appliedDiscountPercentage, netTotal }: PricingSummaryProps) {
  const discountAmount =
    appliedDiscountPercentage !== undefined ? (grossSubtotal * appliedDiscountPercentage) / 100 : undefined

  return (
    <dl className="divide-y divide-gray-100 text-sm">
      <div className="flex justify-between py-2">
        <dt className="text-gray-600">Gross Subtotal</dt>
        <dd>{currency.format(grossSubtotal)}</dd>
      </div>
      <div className="flex justify-between py-2">
        <dt className="text-gray-600">Contract Discount{appliedDiscountPercentage !== undefined ? ` (${appliedDiscountPercentage}%)` : ''}</dt>
        <dd>{discountAmount !== undefined ? `-${currency.format(discountAmount)}` : '—'}</dd>
      </div>
      <div className="flex justify-between py-2">
        <dt className="text-gray-600">Freight &amp; Logistics</dt>
        <dd>Waived</dd>
      </div>
      <div className="flex justify-between py-2 font-semibold text-gray-900">
        <dt>Final Net Total</dt>
        <dd>{netTotal !== undefined ? currency.format(netTotal) : '—'}</dd>
      </div>
    </dl>
  )
}
