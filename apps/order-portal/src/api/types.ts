export type OrderStatus =
  'INTAKE' | 'PROCESSING' | 'BACKORDERED' | 'SHIPPED' | 'FINAL_DELIVERY' | 'CANCELLED'

export interface DemoIdentity {
  id: string
  displayName: string
  role: 'CLIENT' | 'OPERATOR'
  contractReference: string | null
}

export interface HardwareCatalogItem {
  sku: string
  name: string
  listPrice: number
}

export interface LineItemInput {
  sku: string
  quantity: number
}

export interface LineItem extends LineItemInput {
  id: string
  unitListPrice: number
  lineSubtotal: number
}

export interface LifecycleTransition {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  actorType: 'CLIENT' | 'OPERATOR' | 'SYSTEM'
  actorId: string
  occurredAt: string
}

export interface OrderDetail {
  id: string
  clientId: string
  status: OrderStatus
  lineItems: LineItem[]
  grossTotal: number
  appliedDiscountPercentage: number
  netTotal: number
  netTotalLocked: boolean
  createdAt: string
  updatedAt: string
  cancelledAt: string | null
  transitions: LifecycleTransition[]
}

export interface OrderSummary {
  id: string
  status: OrderStatus
  lineItems: LineItem[]
  grossTotal: number
  netTotal: number
  createdAt: string
  updatedAt: string
  transitions: LifecycleTransition[]
}

export interface OrderHistoryPage {
  items: OrderSummary[]
  page: number
  pageSize: number
  totalCount: number
  hasMore: boolean
}
