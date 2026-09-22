package com.compudelivery.orders.order;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Replaces an order's line items while it is Intake or Processing (FR-025,
 * FR-030).
 */
public record ReplaceLineItemsRequest(
		@NotEmpty(message = "Order must contain at least one line item") @Valid @MaxLineItems(100) @ArraySchema(maxItems = 100) List<LineItemInput> lineItems) {
}
