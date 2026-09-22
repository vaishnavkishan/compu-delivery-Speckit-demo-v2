package com.compudelivery.orders.pricing;

/**
 * Client's contract discount terms are missing, ambiguous, or expired at the
 * moment of calculation (FR-004).
 */
public class ContractTermsBlockedException extends RuntimeException {

	public ContractTermsBlockedException(String reason) {
		super(reason);
	}
}
