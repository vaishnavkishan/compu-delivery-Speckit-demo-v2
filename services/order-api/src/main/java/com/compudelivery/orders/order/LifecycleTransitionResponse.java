package com.compudelivery.orders.order;

import java.time.Instant;

public record LifecycleTransitionResponse(OrderStatus fromStatus, OrderStatus toStatus,
		LifecycleTransition.ActorType actorType, String actorId, Instant occurredAt) {

	public static LifecycleTransitionResponse from(LifecycleTransition entity) {
		return new LifecycleTransitionResponse(entity.getFromStatus(), entity.getToStatus(), entity.getActorType(),
				entity.getActorId(), entity.getOccurredAt());
	}
}
