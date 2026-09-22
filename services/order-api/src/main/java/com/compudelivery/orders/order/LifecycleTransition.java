package com.compudelivery.orders.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lifecycle_transition")
@Getter
@Setter
@NoArgsConstructor
public class LifecycleTransition {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private OrderStatus fromStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "to_status", nullable = false)
	private OrderStatus toStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "actor_type", nullable = false)
	private ActorType actorType;

	@Column(name = "actor_id", nullable = false)
	private String actorId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	public enum ActorType {
		CLIENT, OPERATOR, SYSTEM
	}
}
