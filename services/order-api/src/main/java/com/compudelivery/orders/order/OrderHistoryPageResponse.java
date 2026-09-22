package com.compudelivery.orders.order;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * A bounded, newest-first page of a client's own orders (FR-008). {@code
 * hasMore} tells the frontend whether to render the "reach older entries"
 * control without a separate count call.
 */
public record OrderHistoryPageResponse(List<OrderSummaryResponse> items, int page, int pageSize, long totalCount,
		boolean hasMore) {

	public static OrderHistoryPageResponse from(Page<BulkOrder> page, List<OrderSummaryResponse> items) {
		return new OrderHistoryPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(),
				page.hasNext());
	}
}
