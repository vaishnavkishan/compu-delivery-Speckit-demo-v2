package com.compudelivery.orders.web;

import com.compudelivery.orders.order.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * A 409 {@link ProblemDetail} carrying the order's now-current status (FR-016).
 */
public class ConflictProblemDetail extends ProblemDetail {

	private final OrderStatus currentStatus;

	public ConflictProblemDetail(String detail, OrderStatus currentStatus) {
		super();
		setStatus(HttpStatus.CONFLICT);
		setDetail(detail);
		this.currentStatus = currentStatus;
	}

	public OrderStatus getCurrentStatus() {
		return currentStatus;
	}
}
