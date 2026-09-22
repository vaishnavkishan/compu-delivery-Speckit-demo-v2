package com.compudelivery.orders.order;

import com.compudelivery.orders.catalog.HardwareCatalogItem;
import com.compudelivery.orders.client.ContractDiscountTermsLookup;
import com.compudelivery.orders.messaging.OrderIntakenEvent;
import com.compudelivery.orders.pricing.NetTotalCalculation;
import com.compudelivery.orders.pricing.NetTotalCalculationRepository;
import com.compudelivery.orders.pricing.PricingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Validates and accepts a new bulk order into Intake (FR-001–FR-004, FR-014,
 * FR-015, FR-030). SKU/ceiling validation and the contract-terms lookup both
 * run before any row is persisted.
 */
@Service
public class OrderIntakeService {

	private final LineItemAssembler lineItemAssembler;
	private final ContractDiscountTermsLookup contractDiscountTermsLookup;
	private final PricingService pricingService;
	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;
	private final NetTotalCalculationRepository netTotalCalculationRepository;
	private final ApplicationEventPublisher eventPublisher;

	public OrderIntakeService(LineItemAssembler lineItemAssembler,
			ContractDiscountTermsLookup contractDiscountTermsLookup, PricingService pricingService,
			BulkOrderRepository bulkOrderRepository, LineItemRepository lineItemRepository,
			LifecycleTransitionRepository lifecycleTransitionRepository,
			NetTotalCalculationRepository netTotalCalculationRepository, ApplicationEventPublisher eventPublisher) {
		this.lineItemAssembler = lineItemAssembler;
		this.contractDiscountTermsLookup = contractDiscountTermsLookup;
		this.pricingService = pricingService;
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
		this.netTotalCalculationRepository = netTotalCalculationRepository;
		this.eventPublisher = eventPublisher;
	}

	@Transactional
	public OrderDetailResponse createOrder(String clientId, CreateOrderRequest request) {
		Map<String, HardwareCatalogItem> catalog = lineItemAssembler.resolveCatalog(request.lineItems());

		// Contract terms are resolved (and may block the order) before any row is
		// persisted (FR-004).
		BigDecimal discountPercentage = contractDiscountTermsLookup.resolveCurrentDiscountPercentage(clientId);

		Instant now = Instant.now();
		UUID orderId = UUID.randomUUID();

		List<LineItem> lineItems = lineItemAssembler.buildLineItems(orderId, request.lineItems(), catalog);
		BigDecimal grossTotal = lineItemAssembler.sumGrossTotal(lineItems);
		BigDecimal netTotal = pricingService.netTotal(grossTotal, discountPercentage);

		BulkOrder order = new BulkOrder();
		order.setId(orderId);
		order.setClientId(clientId);
		order.setStatus(OrderStatus.INTAKE);
		order.setGrossTotal(grossTotal);
		order.setAppliedDiscountPercentage(discountPercentage);
		order.setNetTotal(netTotal);
		order.setCreatedAt(now);
		order.setUpdatedAt(now);
		bulkOrderRepository.save(order);
		lineItemRepository.saveAll(lineItems);

		LifecycleTransition transition = new LifecycleTransition();
		transition.setId(UUID.randomUUID());
		transition.setOrderId(orderId);
		transition.setFromStatus(null);
		transition.setToStatus(OrderStatus.INTAKE);
		transition.setActorType(LifecycleTransition.ActorType.CLIENT);
		transition.setActorId(clientId);
		transition.setOccurredAt(now);
		lifecycleTransitionRepository.save(transition);

		NetTotalCalculation calculation = new NetTotalCalculation();
		calculation.setId(UUID.randomUUID());
		calculation.setOrderId(orderId);
		calculation.setGrossTotal(grossTotal);
		calculation.setAppliedDiscountPercentage(discountPercentage);
		calculation.setNetTotal(netTotal);
		calculation.setTrigger(NetTotalCalculation.Trigger.INTAKE);
		calculation.setActorId(clientId);
		calculation.setOccurredAt(now);
		netTotalCalculationRepository.save(calculation);

		eventPublisher.publishEvent(new OrderIntakenEvent(orderId, clientId, lineItems.stream()
				.map(li -> new OrderIntakenEvent.LineItemSnapshot(li.getSku(), li.getQuantity(), li.getUnitListPrice()))
				.toList(), grossTotal, discountPercentage, netTotal, now));

		return OrderDetailResponse.from(order, lineItems, List.of(transition));
	}
}
