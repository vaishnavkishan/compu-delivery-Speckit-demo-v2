package com.compudelivery.orders.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BulkOrderRepository extends JpaRepository<BulkOrder, UUID> {

	Page<BulkOrder> findByClientIdOrderByCreatedAtDescIdDesc(String clientId, Pageable pageable);

	Optional<BulkOrder> findByIdAndClientId(UUID id, String clientId);

	/**
	 * A scalar projection, not an entity load: unlike {@code findById}, this always
	 * executes against the database rather than returning an already-managed (and
	 * potentially stale, pre-flush-failure) entity instance from the current
	 * persistence context (FR-016).
	 */
	@Query("select b.status from BulkOrder b where b.id = :orderId")
	Optional<OrderStatus> findCurrentStatusById(@Param("orderId") UUID orderId);
}
