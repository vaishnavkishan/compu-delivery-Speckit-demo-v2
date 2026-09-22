package com.compudelivery.orders.client;

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
@Table(name = "contract_discount_terms")
@Getter
@Setter
@NoArgsConstructor
public class ContractDiscountTerms {

	@Id
	private UUID id;

	@Column(name = "client_id", nullable = false)
	private String clientId;

	@Column(name = "discount_percentage", nullable = false)
	private java.math.BigDecimal discountPercentage;

	@Column(name = "effective_from", nullable = false)
	private Instant effectiveFrom;

	@Column(name = "effective_until")
	private Instant effectiveUntil;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
