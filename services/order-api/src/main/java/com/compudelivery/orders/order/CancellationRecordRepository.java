package com.compudelivery.orders.order;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CancellationRecordRepository extends JpaRepository<CancellationRecord, UUID> {
}
