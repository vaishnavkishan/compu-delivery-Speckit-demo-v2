package com.compudelivery.orders.order;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Centralizes the order lifecycle's allowed-transitions map (FR-005, FR-006;
 * research.md #4). Every status write, forward progression or cancellation,
 * must be validated here first.
 */
@Service
public class OrderLifecycleService {

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(OrderStatus.class);

	static {
		ALLOWED_TRANSITIONS.put(OrderStatus.INTAKE, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
		ALLOWED_TRANSITIONS.put(OrderStatus.PROCESSING,
				EnumSet.of(OrderStatus.BACKORDERED, OrderStatus.SHIPPED, OrderStatus.CANCELLED));
		ALLOWED_TRANSITIONS.put(OrderStatus.BACKORDERED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED));
		ALLOWED_TRANSITIONS.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.FINAL_DELIVERY, OrderStatus.CANCELLED));
		ALLOWED_TRANSITIONS.put(OrderStatus.FINAL_DELIVERY, EnumSet.noneOf(OrderStatus.class));
		ALLOWED_TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
	}

	public boolean isAllowed(OrderStatus from, OrderStatus to) {
		return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
	}

	public void assertAllowed(OrderStatus from, OrderStatus to) {
		if (!isAllowed(from, to)) {
			throw new InvalidLifecycleTransitionException(from, to);
		}
	}

	public boolean isActive(OrderStatus status) {
		return status != OrderStatus.FINAL_DELIVERY && status != OrderStatus.CANCELLED;
	}
}
