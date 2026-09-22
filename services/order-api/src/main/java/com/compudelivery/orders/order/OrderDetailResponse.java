package com.compudelivery.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailResponse(UUID id, String clientId, OrderStatus status, List<LineItemResponse> lineItems,
		BigDecimal grossTotal, BigDecimal appliedDiscountPercentage, BigDecimal netTotal, boolean netTotalLocked,
		Instant createdAt, Instant updatedAt, Instant cancelledAt, List<LifecycleTransitionResponse> transitions) {

	public static OrderDetailResponse from(BulkOrder order, List<LineItem> lineItems,
			List<LifecycleTransition> transitions) {
		return new OrderDetailResponse(order.getId(), order.getClientId(), order.getStatus(),
				lineItems.stream().map(LineItemResponse::from).toList(), order.getGrossTotal(),
				order.getAppliedDiscountPercentage(), order.getNetTotal(), order.getNetTotalLockedAt() != null,
				order.getCreatedAt(), order.getUpdatedAt(), order.getCancelledAt(),
				transitions.stream().map(LifecycleTransitionResponse::from).toList());
	}
}
