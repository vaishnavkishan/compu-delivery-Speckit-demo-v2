package com.compudelivery.orders.order;

/**
 * A first-committed-wins rejection or an otherwise invalid state change,
 * carrying the order's freshly re-read now-current status so the caller sees
 * the actual outcome (FR-016).
 */
public class OrderConflictException extends RuntimeException {

	private final OrderStatus currentStatus;

	public OrderConflictException(String message, OrderStatus currentStatus) {
		super(message);
		this.currentStatus = currentStatus;
	}

	public OrderStatus getCurrentStatus() {
		return currentStatus;
	}
}
