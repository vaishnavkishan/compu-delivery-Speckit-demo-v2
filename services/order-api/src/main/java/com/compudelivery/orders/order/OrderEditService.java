package com.compudelivery.orders.order;

import com.compudelivery.orders.catalog.HardwareCatalogItem;
import com.compudelivery.orders.client.ContractDiscountTermsLookup;
import com.compudelivery.orders.pricing.NetTotalCalculation;
import com.compudelivery.orders.pricing.NetTotalCalculationRepository;
import com.compudelivery.orders.pricing.PricingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces an order's line items while it is Intake or Processing,
 * recalculating Gross/Net Total from currently effective contract terms
 * (FR-025). A rejected edit — ceiling, unknown SKU, blocked terms, or a lock
 * past Processing — leaves the order's existing line items and totals
 * untouched, since nothing is written until every check has passed.
 */
@Service
public class OrderEditService {

	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;
	private final NetTotalCalculationRepository netTotalCalculationRepository;
	private final LineItemAssembler lineItemAssembler;
	private final ContractDiscountTermsLookup contractDiscountTermsLookup;
	private final PricingService pricingService;

	public OrderEditService(BulkOrderRepository bulkOrderRepository, LineItemRepository lineItemRepository,
			LifecycleTransitionRepository lifecycleTransitionRepository,
			NetTotalCalculationRepository netTotalCalculationRepository, LineItemAssembler lineItemAssembler,
			ContractDiscountTermsLookup contractDiscountTermsLookup, PricingService pricingService) {
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
		this.netTotalCalculationRepository = netTotalCalculationRepository;
		this.lineItemAssembler = lineItemAssembler;
		this.contractDiscountTermsLookup = contractDiscountTermsLookup;
		this.pricingService = pricingService;
	}

	@Transactional
	public OrderDetailResponse replaceLineItems(String clientId, UUID orderId, ReplaceLineItemsRequest request) {
		BulkOrder order = bulkOrderRepository.findByIdAndClientId(orderId, clientId)
				.orElseThrow(OrderNotFoundException::new);

		if (order.getStatus() != OrderStatus.INTAKE && order.getStatus() != OrderStatus.PROCESSING) {
			throw new OrderConflictException("Order is no longer editable once it has advanced beyond Processing",
					order.getStatus());
		}

		Map<String, HardwareCatalogItem> catalog = lineItemAssembler.resolveCatalog(request.lineItems());
		BigDecimal discountPercentage = contractDiscountTermsLookup.resolveCurrentDiscountPercentage(clientId);

		List<LineItem> newLineItems = lineItemAssembler.buildLineItems(orderId, request.lineItems(), catalog);
		BigDecimal grossTotal = lineItemAssembler.sumGrossTotal(newLineItems);
		BigDecimal netTotal = pricingService.netTotal(grossTotal, discountPercentage);

		lineItemRepository.deleteByOrderId(orderId);
		lineItemRepository.saveAll(newLineItems);

		Instant now = Instant.now();
		order.setGrossTotal(grossTotal);
		order.setAppliedDiscountPercentage(discountPercentage);
		order.setNetTotal(netTotal);
		order.setUpdatedAt(now);
		bulkOrderRepository.save(order);

		NetTotalCalculation calculation = new NetTotalCalculation();
		calculation.setId(UUID.randomUUID());
		calculation.setOrderId(orderId);
		calculation.setGrossTotal(grossTotal);
		calculation.setAppliedDiscountPercentage(discountPercentage);
		calculation.setNetTotal(netTotal);
		calculation.setTrigger(NetTotalCalculation.Trigger.LINE_ITEM_EDIT);
		calculation.setActorId(clientId);
		calculation.setOccurredAt(now);
		netTotalCalculationRepository.save(calculation);

		List<LifecycleTransition> transitions = lifecycleTransitionRepository
				.findByOrderIdOrderByOccurredAtAsc(orderId);
		return OrderDetailResponse.from(order, newLineItems, transitions);
	}
}
