package com.compudelivery.orders.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes {@code OrderIntaken} to {@code order.events}/{@code order.intaken}
 * only after the creating transaction commits, so a message is never published
 * for an order that failed to persist (research.md #7).
 */
@Component
public class OrderIntakenPublisher {

	private final RabbitTemplate rabbitTemplate;

	public OrderIntakenPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderIntaken(OrderIntakenEvent event) {
		rabbitTemplate.convertAndSend(RabbitTopologyConfig.ORDER_EVENTS_EXCHANGE,
				RabbitTopologyConfig.ORDER_INTAKEN_ROUTING_KEY, OrderIntakenPayload.from(event));
	}
}
