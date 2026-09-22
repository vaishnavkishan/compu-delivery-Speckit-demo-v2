package com.compudelivery.orders.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "hardware_catalog_item")
@Getter
@Setter
@NoArgsConstructor
public class HardwareCatalogItem {

	@Id
	private String sku;

	@Column(nullable = false)
	private String name;

	@Column(name = "list_price", nullable = false)
	private BigDecimal listPrice;
}
