import type { OrderStatus } from '../api/types'

const STEPS: { status: OrderStatus; label: string }[] = [
  { status: 'INTAKE', label: 'Intake' },
  { status: 'PROCESSING', label: 'Processing' },
  { status: 'BACKORDERED', label: 'Backordered' },
  { status: 'SHIPPED', label: 'Shipped' },
  { status: 'FINAL_DELIVERY', label: 'Final Delivery' },
]

interface LifecycleStepperProps {
  status: OrderStatus
}

/** Permanent 5-column Intake/Processing/Backordered/Shipped/Final Delivery indicator (FR-022). */
export function LifecycleStepper({ status }: LifecycleStepperProps) {
  const currentIndex = STEPS.findIndex((step) => step.status === status)

  return (
    <ol className="flex items-center gap-1" aria-label="Order lifecycle progress">
      {STEPS.map((step, index) => {
        const isComplete = currentIndex >= 0 && index < currentIndex
        const isCurrent = index === currentIndex
        return (
          <li
            key={step.status}
            aria-current={isCurrent ? 'step' : undefined}
            className="flex flex-1 flex-col items-center gap-1 text-center"
          >
            <span
              aria-hidden="true"
              className={`h-2 w-full rounded-full ${
                isComplete || isCurrent ? 'bg-indigo-600' : 'bg-gray-200'
              }`}
            />
            <span
              className={`text-xs ${isCurrent ? 'font-semibold text-gray-900' : 'text-gray-500'}`}
            >
              {step.label}
            </span>
          </li>
        )
      })}
    </ol>
  )
}
