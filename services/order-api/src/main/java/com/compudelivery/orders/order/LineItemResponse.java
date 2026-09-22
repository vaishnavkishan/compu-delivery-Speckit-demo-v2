package com.compudelivery.orders.order;

import java.math.BigDecimal;
import java.util.UUID;

public record LineItemResponse(UUID id, String sku, Integer quantity, BigDecimal unitListPrice,
		BigDecimal lineSubtotal) {

	public static LineItemResponse from(LineItem entity) {
		return new LineItemResponse(entity.getId(), entity.getSku(), entity.getQuantity(), entity.getUnitListPrice(),
				entity.getLineSubtotal());
	}
}
