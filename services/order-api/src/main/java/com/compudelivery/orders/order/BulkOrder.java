package com.compudelivery.orders.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bulk_order")
@Getter
@Setter
@NoArgsConstructor
public class BulkOrder {

	@Id
	private UUID id;

	@Column(name = "client_id", nullable = false)
	private String clientId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderStatus status;

	@Column(name = "gross_total", nullable = false)
	private BigDecimal grossTotal;

	@Column(name = "applied_discount_percentage", nullable = false)
	private BigDecimal appliedDiscountPercentage;

	@Column(name = "net_total", nullable = false)
	private BigDecimal netTotal;

	@Column(name = "net_total_locked_at")
	private Instant netTotalLockedAt;

	@Version
	@Column(nullable = false)
	private Long version;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Column(name = "cancelled_at")
	private Instant cancelledAt;
}
