package com.compudelivery.orders.identity;

public record DemoIdentity(String id, String displayName, Role role) {

	public enum Role {
		CLIENT, OPERATOR
	}
}
