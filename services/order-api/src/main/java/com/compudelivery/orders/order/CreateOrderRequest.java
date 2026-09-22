package com.compudelivery.orders.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Submits a new bulk order (FR-001, FR-015, FR-030). */
public record CreateOrderRequest(
		@NotEmpty(message = "Order must contain at least one line item") @Valid @MaxLineItems(100) List<LineItemInput> lineItems) {
}
