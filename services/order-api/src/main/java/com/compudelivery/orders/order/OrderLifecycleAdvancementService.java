package com.compudelivery.orders.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Advances an order's lifecycle status on an operator's behalf (FR-012),
 * delegating skip/backward/terminal-state enforcement to
 * {@link OrderLifecycleService}. Locks the Net Total once the target status is
 * Shipped (FR-017, `data-model.md`'s field spec).
 */
@Service
public class OrderLifecycleAdvancementService {

	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;
	private final OrderLifecycleService orderLifecycleService;

	public OrderLifecycleAdvancementService(BulkOrderRepository bulkOrderRepository,
			LineItemRepository lineItemRepository, LifecycleTransitionRepository lifecycleTransitionRepository,
			OrderLifecycleService orderLifecycleService) {
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
		this.orderLifecycleService = orderLifecycleService;
	}

	@Transactional
	public OrderDetailResponse advance(String operatorId, UUID orderId, OrderStatus targetStatus) {
		BulkOrder order = bulkOrderRepository.findById(orderId).orElseThrow(OrderNotFoundException::new);

		OrderStatus previousStatus = order.getStatus();
		orderLifecycleService.assertAllowed(previousStatus, targetStatus);

		Instant now = Instant.now();

		LifecycleTransition transition = new LifecycleTransition();
		transition.setId(UUID.randomUUID());
		transition.setOrderId(orderId);
		transition.setFromStatus(previousStatus);
		transition.setToStatus(targetStatus);
		transition.setActorType(LifecycleTransition.ActorType.OPERATOR);
		transition.setActorId(operatorId);
		transition.setOccurredAt(now);
		lifecycleTransitionRepository.save(transition);

		order.setStatus(targetStatus);
		if (targetStatus == OrderStatus.SHIPPED) {
			order.setNetTotalLockedAt(now);
		}
		order.setUpdatedAt(now);
		bulkOrderRepository.save(order);

		List<LineItem> lineItems = lineItemRepository.findByOrderId(orderId);
		List<LifecycleTransition> transitions = lifecycleTransitionRepository
				.findByOrderIdOrderByOccurredAtAsc(orderId);
		return OrderDetailResponse.from(order, lineItems, transitions);
	}
}
