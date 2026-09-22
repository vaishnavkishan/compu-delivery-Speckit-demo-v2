package com.compudelivery.orders.identity;

/**
 * The trusted caller identity resolved from
 * {@code X-Client-Id}/{@code X-Operator-Id} for the current request. Either
 * field may be null; each endpoint enforces which one(s) it actually requires
 * (see research.md #1).
 */
public record CallerIdentity(String clientId, String operatorId) {

	public boolean hasClient() {
		return clientId != null;
	}

	public boolean hasOperator() {
		return operatorId != null;
	}

	public String requireClientId() {
		if (clientId == null) {
			throw new MissingIdentityException("X-Client-Id header is required for this request");
		}
		return clientId;
	}

	public String requireOperatorId() {
		if (operatorId == null) {
			throw new MissingIdentityException("X-Operator-Id header is required for this request");
		}
		return operatorId;
	}
}
