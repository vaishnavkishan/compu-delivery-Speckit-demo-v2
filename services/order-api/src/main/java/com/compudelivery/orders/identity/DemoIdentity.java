package com.compudelivery.orders.identity;

public record DemoIdentity(String id, String displayName, Role role, String contractReference) {

	public enum Role {
		CLIENT, OPERATOR
	}
}
