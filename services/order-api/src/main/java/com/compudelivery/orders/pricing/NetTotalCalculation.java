package com.compudelivery.orders.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "net_total_calculation")
@Getter
@Setter
@NoArgsConstructor
public class NetTotalCalculation {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(name = "gross_total", nullable = false)
	private BigDecimal grossTotal;

	@Column(name = "applied_discount_percentage", nullable = false)
	private BigDecimal appliedDiscountPercentage;

	@Column(name = "net_total", nullable = false)
	private BigDecimal netTotal;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Trigger trigger;

	@Column(name = "actor_id", nullable = false)
	private String actorId;

	@Column(name = "occurred_at", nullable = false)
	private Instant occurredAt;

	public enum Trigger {
		INTAKE, LINE_ITEM_EDIT
	}
}
