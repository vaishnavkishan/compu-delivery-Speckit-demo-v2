package com.compudelivery.orders.order;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Reads a single order's detail, scoped to the owning client so a nonexistent
 * order and one owned by a different client are indistinguishable (FR-018).
 */
@Service
public class OrderQueryService {

	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;

	public OrderQueryService(BulkOrderRepository bulkOrderRepository, LineItemRepository lineItemRepository,
			LifecycleTransitionRepository lifecycleTransitionRepository) {
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
	}

	public OrderDetailResponse getOrder(String clientId, UUID orderId) {
		BulkOrder order = bulkOrderRepository.findByIdAndClientId(orderId, clientId)
				.orElseThrow(OrderNotFoundException::new);
		List<LineItem> lineItems = lineItemRepository.findByOrderId(orderId);
		List<LifecycleTransition> transitions = lifecycleTransitionRepository
				.findByOrderIdOrderByOccurredAtAsc(orderId);
		return OrderDetailResponse.from(order, lineItems, transitions);
	}
}
