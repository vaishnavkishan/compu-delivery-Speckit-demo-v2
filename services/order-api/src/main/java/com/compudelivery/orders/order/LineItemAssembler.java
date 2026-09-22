package com.compudelivery.orders.order;

import com.compudelivery.orders.catalog.HardwareCatalogItem;
import com.compudelivery.orders.catalog.HardwareCatalogItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Shared SKU-resolution and line-item-building logic for order intake and
 * line-item edits.
 */
@Component
public class LineItemAssembler {

	private final HardwareCatalogItemRepository hardwareCatalogItemRepository;

	public LineItemAssembler(HardwareCatalogItemRepository hardwareCatalogItemRepository) {
		this.hardwareCatalogItemRepository = hardwareCatalogItemRepository;
	}

	/**
	 * @throws InvalidOrderRequestException
	 *             if any requested SKU is not in the catalog (FR-014).
	 */
	public Map<String, HardwareCatalogItem> resolveCatalog(List<LineItemInput> lineItems) {
		List<String> skus = lineItems.stream().map(LineItemInput::sku).distinct().toList();
		Map<String, HardwareCatalogItem> catalog = hardwareCatalogItemRepository.findAllById(skus).stream()
				.collect(Collectors.toMap(HardwareCatalogItem::getSku, item -> item));
		for (String sku : skus) {
			if (!catalog.containsKey(sku)) {
				throw new InvalidOrderRequestException("Unknown hardware SKU: " + sku);
			}
		}
		return catalog;
	}

	public List<LineItem> buildLineItems(UUID orderId, List<LineItemInput> inputs,
			Map<String, HardwareCatalogItem> catalog) {
		return inputs.stream().map(input -> {
			HardwareCatalogItem catalogItem = catalog.get(input.sku());
			LineItem lineItem = new LineItem();
			lineItem.setId(UUID.randomUUID());
			lineItem.setOrderId(orderId);
			lineItem.setSku(input.sku());
			lineItem.setQuantity(input.quantity());
			lineItem.setUnitListPrice(catalogItem.getListPrice());
			lineItem.setLineSubtotal(catalogItem.getListPrice().multiply(BigDecimal.valueOf(input.quantity())));
			return lineItem;
		}).toList();
	}

	public BigDecimal sumGrossTotal(List<LineItem> lineItems) {
		return lineItems.stream().map(LineItem::getLineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
