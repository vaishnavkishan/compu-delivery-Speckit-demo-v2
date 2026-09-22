package com.compudelivery.orders.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BulkOrderRepository extends JpaRepository<BulkOrder, UUID> {

	Page<BulkOrder> findByClientIdOrderByCreatedAtDescIdDesc(String clientId, Pageable pageable);

	Optional<BulkOrder> findByIdAndClientId(UUID id, String clientId);
}
