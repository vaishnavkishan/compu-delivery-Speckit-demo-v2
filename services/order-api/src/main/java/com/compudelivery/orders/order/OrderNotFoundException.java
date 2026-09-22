package com.compudelivery.orders.order;

/**
 * Order does not exist, or exists but belongs to a different client — identical
 * response either way (FR-018).
 */
public class OrderNotFoundException extends RuntimeException {

	public OrderNotFoundException() {
		super("Order not found");
	}
}
