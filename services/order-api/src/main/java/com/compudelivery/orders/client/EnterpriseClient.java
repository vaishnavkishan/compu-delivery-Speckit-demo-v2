package com.compudelivery.orders.client;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "enterprise_client")
@Getter
@Setter
@NoArgsConstructor
public class EnterpriseClient {

	@Id
	@Column(name = "client_id")
	private String clientId;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "contract_reference")
	private String contractReference;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;
}
