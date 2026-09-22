package com.compudelivery.orders.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineItemRepository extends JpaRepository<LineItem, UUID> {

	List<LineItem> findByOrderId(UUID orderId);

	List<LineItem> findByOrderIdIn(List<UUID> orderIds);

	void deleteByOrderId(UUID orderId);
}
