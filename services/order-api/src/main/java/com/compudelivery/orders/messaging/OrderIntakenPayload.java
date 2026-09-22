package com.compudelivery.orders.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JSON wire shape for the {@code OrderIntaken} event, matching
 * contracts/events.md exactly.
 */
public record OrderIntakenPayload(String eventType, UUID eventId, Instant occurredAt, UUID orderId, String clientId,
		List<OrderIntakenEvent.LineItemSnapshot> lineItems, BigDecimal grossTotal, BigDecimal appliedDiscountPercentage,
		BigDecimal netTotal) {

	public static OrderIntakenPayload from(OrderIntakenEvent event) {
		return new OrderIntakenPayload("OrderIntaken", UUID.randomUUID(), event.occurredAt(), event.orderId(),
				event.clientId(), event.lineItems(), event.grossTotal(), event.appliedDiscountPercentage(),
				event.netTotal());
	}
}
