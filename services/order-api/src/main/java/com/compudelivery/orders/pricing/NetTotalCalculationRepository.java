package com.compudelivery.orders.pricing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NetTotalCalculationRepository extends JpaRepository<NetTotalCalculation, UUID> {

	List<NetTotalCalculation> findByOrderIdOrderByOccurredAtAsc(UUID orderId);
}
