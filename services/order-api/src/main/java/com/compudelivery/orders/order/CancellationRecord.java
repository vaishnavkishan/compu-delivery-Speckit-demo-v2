package com.compudelivery.orders.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cancellation_record")
@Getter
@Setter
@NoArgsConstructor
public class CancellationRecord {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false, unique = true)
	private UUID orderId;

	@Column(name = "cancelled_by", nullable = false)
	private String cancelledBy;

	@Column(name = "cancelled_at", nullable = false)
	private Instant cancelledAt;
}
