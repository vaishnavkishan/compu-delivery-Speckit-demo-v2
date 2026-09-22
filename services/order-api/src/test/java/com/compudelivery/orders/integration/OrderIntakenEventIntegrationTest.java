package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.messaging.OrderIntakenPayload;
import com.compudelivery.orders.messaging.RabbitTopologyConfig;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Confirms {@code OrderIntaken} is published to
 * {@code order.events}/{@code order.intaken} only after the creating
 * transaction commits, matching contracts/events.md (research.md #7).
 */
class OrderIntakenEventIntegrationTest extends IntegrationTestBase {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	private void drainQueue() {
		while (rabbitTemplate.receive(RabbitTopologyConfig.ORDER_INTAKEN_QUEUE, 100) != null) {
			// discard any messages left over from other tests sharing this queue
		}
	}

	@Test
	void publishesOrderIntakenAfterCommitMatchingTheEventContract() {
		drainQueue();

		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", "ACME-001");
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 3)));

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, headers), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		String orderId = (String) response.getBody().get("id");

		Message message = rabbitTemplate.receive(RabbitTopologyConfig.ORDER_INTAKEN_QUEUE, 5000);
		assertThat(message).isNotNull();

		// The wire payload's occurredAt must be the ISO-8601 string contracts/events.md
		// documents, not Jackson's default epoch-seconds-and-nanos numeric timestamp
		// (research.md's date-handling convention, matched to the REST API's
		// ISO-8601 responses).
		String rawJson = new String(message.getBody(), StandardCharsets.UTF_8);
		assertThat(rawJson).containsPattern("\"occurredAt\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}");

		Object received = rabbitTemplate.getMessageConverter().fromMessage(message);
		assertThat(received).isInstanceOf(OrderIntakenPayload.class);
		OrderIntakenPayload payload = (OrderIntakenPayload) received;
		assertThat(payload.eventType()).isEqualTo("OrderIntaken");
		assertThat(payload.orderId()).hasToString(orderId);
		assertThat(payload.clientId()).isEqualTo("ACME-001");
		assertThat(payload.lineItems()).hasSize(1);
		assertThat(payload.netTotal())
				.isEqualByComparingTo(new BigDecimal(response.getBody().get("netTotal").toString()));
	}
}
