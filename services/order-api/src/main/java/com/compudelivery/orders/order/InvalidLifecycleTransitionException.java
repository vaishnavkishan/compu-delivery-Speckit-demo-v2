package com.compudelivery.orders.order;

/**
 * Thrown when a requested status transition skips a stage, moves backward, or
 * leaves a terminal state (FR-006).
 */
public class InvalidLifecycleTransitionException extends RuntimeException {

	private final OrderStatus currentStatus;

	public InvalidLifecycleTransitionException(OrderStatus from, OrderStatus to) {
		super("Cannot transition order from " + from + " to " + to);
		this.currentStatus = from;
	}

	public OrderStatus getCurrentStatus() {
		return currentStatus;
	}
}
