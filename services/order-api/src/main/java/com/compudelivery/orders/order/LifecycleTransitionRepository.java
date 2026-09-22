package com.compudelivery.orders.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LifecycleTransitionRepository extends JpaRepository<LifecycleTransition, UUID> {

	List<LifecycleTransition> findByOrderIdOrderByOccurredAtAsc(UUID orderId);
}
