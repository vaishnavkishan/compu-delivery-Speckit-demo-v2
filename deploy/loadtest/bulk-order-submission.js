import http from 'k6/http'
import { check } from 'k6'

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'

// Two seeded clients with valid, differing contract discounts (FR-027) — order
// submissions are spread across multiple client identities rather than
// hammering a single one.
const CLIENTS = [
  { id: 'ACME-001', discountPercentage: 12.5 },
  { id: 'GLOBEX-002', discountPercentage: 8.0 },
]

const SKU = 'SKU-1001'
const UNIT_LIST_PRICE = 4200.0
const QUANTITY = 1

function expectedNetTotal(discountPercentage) {
  const gross = UNIT_LIST_PRICE * QUANTITY
  const net = gross - (gross * discountPercentage) / 100
  // Full precision through the discount step, round-half-up once at the end (FR-003).
  return Math.round(net * 100) / 100
}

export const options = {
  // SC-007: sustain >=500 bulk order submissions/day across all clients without
  // degrading SC-001 (under-5s response) or SC-002 (100%-accurate Net Total
  // rounding). A full 24h day is compressed into a short but still-paced
  // (non-burst) window for practical local/CI execution: the same total
  // volume (500 iterations), spread evenly at a constant arrival rate rather
  // than fired all at once.
  scenarios: {
    daily_order_volume: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '2m',
      duration: '2m',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    http_req_duration: ['max<5000'], // SC-001: every submission stays under 5s.
    checks: ['rate==1.0'], // SC-002: every submission's outcome checks pass.
  },
}

export default function () {
  const client = CLIENTS[Math.floor(Math.random() * CLIENTS.length)]
  const payload = JSON.stringify({ lineItems: [{ sku: SKU, quantity: QUANTITY }] })

  const res = http.post(`${BASE_URL}/api/orders`, payload, {
    headers: { 'Content-Type': 'application/json', 'X-Client-Id': client.id },
  })

  const expectedNet = expectedNetTotal(client.discountPercentage)

  check(res, {
    'status is 201 (order accepted into Intake)': (r) => r.status === 201,
    'responds within 5s (SC-001)': (r) => r.timings.duration < 5000,
    'Net Total is 100% accurate per FR-003 (SC-002)': (r) => {
      const body = JSON.parse(r.body)
      return Math.abs(parseFloat(body.netTotal) - expectedNet) < 0.001
    },
  })
}
