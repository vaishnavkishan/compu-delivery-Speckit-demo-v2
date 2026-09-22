package com.compudelivery.orders.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the {@code order.events} topic exchange and the durable
 * {@code order.events.intaken} queue so {@code OrderIntaken} events are durably
 * captured even before a real consumer exists (contracts/events.md).
 */
@Configuration
public class RabbitTopologyConfig {

	public static final String ORDER_EVENTS_EXCHANGE = "order.events";
	public static final String ORDER_INTAKEN_ROUTING_KEY = "order.intaken";
	public static final String ORDER_INTAKEN_QUEUE = "order.events.intaken";

	@Bean
	public TopicExchange orderEventsExchange() {
		return new TopicExchange(ORDER_EVENTS_EXCHANGE, true, false);
	}

	@Bean
	public Queue orderIntakenQueue() {
		return new Queue(ORDER_INTAKEN_QUEUE, true);
	}

	@Bean
	public Binding orderIntakenBinding(Queue orderIntakenQueue, TopicExchange orderEventsExchange) {
		return BindingBuilder.bind(orderIntakenQueue).to(orderEventsExchange).with(ORDER_INTAKEN_ROUTING_KEY);
	}
}
