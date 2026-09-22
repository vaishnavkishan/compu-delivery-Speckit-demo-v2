package com.compudelivery.orders.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Raised inside the order-creation transaction; {@link OrderIntakenPublisher}
 * publishes it to RabbitMQ only after that transaction commits (research.md
 * #7).
 */
public record OrderIntakenEvent(UUID orderId, String clientId, List<LineItemSnapshot> lineItems, BigDecimal grossTotal,
		BigDecimal appliedDiscountPercentage, BigDecimal netTotal, Instant occurredAt) {

	public record LineItemSnapshot(String sku, int quantity, BigDecimal unitListPrice) {
	}
}
