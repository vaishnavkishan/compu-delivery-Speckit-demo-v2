package com.compudelivery.orders.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * One requested SKU + quantity within a create/edit request (FR-001, FR-014).
 */
public record LineItemInput(@NotBlank(message = "Line item sku must not be blank") String sku,
		@Min(value = 1, message = "Line item quantity must be at least 1") @Max(value = 10000, message = "Line item quantity cannot exceed 10,000 units") Integer quantity) {
}
