package com.compudelivery.orders.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Lists a client's own orders, newest-first, bounded to 25 entries per page
 * (FR-008, FR-009).
 */
@Service
public class OrderHistoryService {

	private static final int PAGE_SIZE = 25;

	private final BulkOrderRepository bulkOrderRepository;

	public OrderHistoryService(BulkOrderRepository bulkOrderRepository) {
		this.bulkOrderRepository = bulkOrderRepository;
	}

	public OrderHistoryPageResponse listOrders(String clientId, int page) {
		Page<BulkOrder> result = bulkOrderRepository.findByClientIdOrderByCreatedAtDescIdDesc(clientId,
				PageRequest.of(page, PAGE_SIZE));
		return OrderHistoryPageResponse.from(result);
	}
}
