package com.compudelivery.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderSummaryResponse(UUID id, OrderStatus status, List<LineItemResponse> lineItems, BigDecimal grossTotal,
		BigDecimal netTotal, Instant createdAt, Instant updatedAt, List<LifecycleTransitionResponse> transitions) {

	public static OrderSummaryResponse from(BulkOrder order, List<LineItem> lineItems,
			List<LifecycleTransition> transitions) {
		return new OrderSummaryResponse(order.getId(), order.getStatus(),
				lineItems.stream().map(LineItemResponse::from).toList(), order.getGrossTotal(), order.getNetTotal(),
				order.getCreatedAt(), order.getUpdatedAt(),
				transitions.stream().map(LifecycleTransitionResponse::from).toList());
	}
}
