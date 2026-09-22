package com.compudelivery.orders.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "line_item")
@Getter
@Setter
@NoArgsConstructor
public class LineItem {

	@Id
	private UUID id;

	@Column(name = "order_id", nullable = false)
	private UUID orderId;

	@Column(nullable = false)
	private String sku;

	@Column(nullable = false)
	private Integer quantity;

	@Column(name = "unit_list_price", nullable = false)
	private BigDecimal unitListPrice;

	@Column(name = "line_subtotal", nullable = false)
	private BigDecimal lineSubtotal;
}
