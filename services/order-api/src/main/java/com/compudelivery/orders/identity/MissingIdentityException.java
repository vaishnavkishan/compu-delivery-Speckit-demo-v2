package com.compudelivery.orders.identity;

public class MissingIdentityException extends RuntimeException {

	public MissingIdentityException(String message) {
		super(message);
	}
}
