package com.compudelivery.orders.order;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Lists a client's own orders, newest-first, bounded to 25 entries per page
 * (FR-008, FR-009). Each entry inlines its line items, Gross Total, and full
 * lifecycle transition history so the history log doesn't require a per-order
 * detail call to render those details (FR-008).
 */
@Service
public class OrderHistoryService {

	private static final int PAGE_SIZE = 25;

	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;

	public OrderHistoryService(BulkOrderRepository bulkOrderRepository, LineItemRepository lineItemRepository,
			LifecycleTransitionRepository lifecycleTransitionRepository) {
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
	}

	public OrderHistoryPageResponse listOrders(String clientId, int page) {
		Page<BulkOrder> result = bulkOrderRepository.findByClientIdOrderByCreatedAtDescIdDesc(clientId,
				PageRequest.of(page, PAGE_SIZE));

		List<UUID> orderIds = result.getContent().stream().map(BulkOrder::getId).toList();
		Map<UUID, List<LineItem>> lineItemsByOrderId = lineItemRepository.findByOrderIdIn(orderIds).stream()
				.collect(Collectors.groupingBy(LineItem::getOrderId));
		Map<UUID, List<LifecycleTransition>> transitionsByOrderId = lifecycleTransitionRepository
				.findByOrderIdInOrderByOccurredAtAsc(orderIds).stream()
				.collect(Collectors.groupingBy(LifecycleTransition::getOrderId));

		List<OrderSummaryResponse> items = result.getContent().stream()
				.map(order -> OrderSummaryResponse.from(order,
						lineItemsByOrderId.getOrDefault(order.getId(), List.of()),
						transitionsByOrderId.getOrDefault(order.getId(), List.of())))
				.toList();

		return OrderHistoryPageResponse.from(result, items);
	}
}
