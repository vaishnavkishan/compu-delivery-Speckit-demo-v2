package com.compudelivery.orders.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMessagingConfig {

	/**
	 * Reuses the application's Spring-Boot-configured {@link ObjectMapper}
	 * (ISO-8601 timestamps, no {@code WRITE_DATES_AS_TIMESTAMPS}) rather than a
	 * bare converter default, so {@code OrderIntaken}'s {@code occurredAt} matches
	 * {@code contracts/events.md}'s documented ISO-8601 shape instead of a raw
	 * epoch number.
	 */
	@Bean
	public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
		Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
		DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
		typeMapper.setTrustedPackages(OrderIntakenPayload.class.getPackageName());
		converter.setJavaTypeMapper(typeMapper);
		return converter;
	}
}
