package com.compudelivery.orders.order;

/**
 * A submitted/edited order fails a structural rule — unknown SKU, empty order,
 * or line-item ceiling (FR-014, FR-015, FR-030).
 */
public class InvalidOrderRequestException extends RuntimeException {

	public InvalidOrderRequestException(String message) {
		super(message);
	}
}
