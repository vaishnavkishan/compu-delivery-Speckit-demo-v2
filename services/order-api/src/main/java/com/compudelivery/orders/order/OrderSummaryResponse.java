package com.compudelivery.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(UUID id, OrderStatus status, BigDecimal netTotal, Instant createdAt,
		Instant updatedAt) {

	public static OrderSummaryResponse from(BulkOrder order) {
		return new OrderSummaryResponse(order.getId(), order.getStatus(), order.getNetTotal(), order.getCreatedAt(),
				order.getUpdatedAt());
	}
}
