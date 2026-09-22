package com.compudelivery.orders.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels one of a client's own Active orders (FR-010, FR-011). Any non-Active
 * order — already Cancelled or at Final Delivery — is rejected via the same
 * centralized transitions map used for forward progression, so "already
 * cancelled" and "already delivered" both surface as a 409 carrying the order's
 * current status.
 */
@Service
public class OrderCancellationService {

	private final BulkOrderRepository bulkOrderRepository;
	private final LineItemRepository lineItemRepository;
	private final LifecycleTransitionRepository lifecycleTransitionRepository;
	private final CancellationRecordRepository cancellationRecordRepository;
	private final OrderLifecycleService orderLifecycleService;

	public OrderCancellationService(BulkOrderRepository bulkOrderRepository, LineItemRepository lineItemRepository,
			LifecycleTransitionRepository lifecycleTransitionRepository,
			CancellationRecordRepository cancellationRecordRepository, OrderLifecycleService orderLifecycleService) {
		this.bulkOrderRepository = bulkOrderRepository;
		this.lineItemRepository = lineItemRepository;
		this.lifecycleTransitionRepository = lifecycleTransitionRepository;
		this.cancellationRecordRepository = cancellationRecordRepository;
		this.orderLifecycleService = orderLifecycleService;
	}

	@Transactional
	public OrderDetailResponse cancel(String clientId, UUID orderId) {
		BulkOrder order = bulkOrderRepository.findByIdAndClientId(orderId, clientId)
				.orElseThrow(OrderNotFoundException::new);

		orderLifecycleService.assertAllowed(order.getStatus(), OrderStatus.CANCELLED);

		Instant now = Instant.now();
		OrderStatus previousStatus = order.getStatus();

		CancellationRecord record = new CancellationRecord();
		record.setId(UUID.randomUUID());
		record.setOrderId(orderId);
		record.setCancelledBy(clientId);
		record.setCancelledAt(now);
		cancellationRecordRepository.save(record);

		LifecycleTransition transition = new LifecycleTransition();
		transition.setId(UUID.randomUUID());
		transition.setOrderId(orderId);
		transition.setFromStatus(previousStatus);
		transition.setToStatus(OrderStatus.CANCELLED);
		transition.setActorType(LifecycleTransition.ActorType.CLIENT);
		transition.setActorId(clientId);
		transition.setOccurredAt(now);
		lifecycleTransitionRepository.save(transition);

		order.setStatus(OrderStatus.CANCELLED);
		order.setCancelledAt(now);
		order.setUpdatedAt(now);
		try {
			bulkOrderRepository.saveAndFlush(order);
		} catch (ObjectOptimisticLockingFailureException ex) {
			OrderStatus currentStatus = bulkOrderRepository.findCurrentStatusById(orderId).orElse(null);
			throw new OrderConflictException("The order was concurrently modified; please retry", currentStatus);
		}

		List<LineItem> lineItems = lineItemRepository.findByOrderId(orderId);
		List<LifecycleTransition> transitions = lifecycleTransitionRepository
				.findByOrderIdOrderByOccurredAtAsc(orderId);
		return OrderDetailResponse.from(order, lineItems, transitions);
	}
}
