package com.compudelivery.orders.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the fixed, non-editable Hardware Catalog (FR-020). */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

	private final HardwareCatalogItemRepository hardwareCatalogItemRepository;

	public CatalogController(HardwareCatalogItemRepository hardwareCatalogItemRepository) {
		this.hardwareCatalogItemRepository = hardwareCatalogItemRepository;
	}

	@GetMapping
	public List<HardwareCatalogItem> listCatalogItems() {
		return hardwareCatalogItemRepository.findAll();
	}
}
