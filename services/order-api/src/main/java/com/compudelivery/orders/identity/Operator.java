package com.compudelivery.orders.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "operator")
@Getter
@Setter
@NoArgsConstructor
public class Operator {

	@Id
	@Column(name = "operator_id")
	private String operatorId;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
