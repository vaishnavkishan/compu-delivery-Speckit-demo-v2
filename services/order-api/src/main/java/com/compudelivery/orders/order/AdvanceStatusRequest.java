package com.compudelivery.orders.order;

import jakarta.validation.constraints.NotNull;

/**
 * Requests an operator-initiated lifecycle status advancement (FR-005, FR-006,
 * FR-012).
 */
public record AdvanceStatusRequest(@NotNull(message = "targetStatus is required") OrderStatus targetStatus) {
}
