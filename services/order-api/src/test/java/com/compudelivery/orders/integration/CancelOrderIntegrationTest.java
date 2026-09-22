package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.order.BulkOrder;
import com.compudelivery.orders.order.BulkOrderRepository;
import com.compudelivery.orders.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** FR-010, FR-011, FR-018. */
class CancelOrderIntegrationTest extends IntegrationTestBase {

	@Autowired
	private BulkOrderRepository bulkOrderRepository;

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private Map createOrder(String clientId) {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders(clientId)), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	private ResponseEntity<Map> cancel(String orderId, String clientId) {
		return restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/cancel", HttpMethod.POST,
				new HttpEntity<>(clientHeaders(clientId)), Map.class);
	}

	@ParameterizedTest
	@EnumSource(value = OrderStatus.class, names = {"INTAKE", "PROCESSING", "BACKORDERED", "SHIPPED"})
	void succeedsFromAnyActiveState(OrderStatus activeStatus) {
		Map created = createOrder("ACME-001");
		UUID orderId = UUID.fromString((String) created.get("id"));
		BulkOrder order = bulkOrderRepository.findById(orderId).orElseThrow();
		order.setStatus(activeStatus);
		bulkOrderRepository.save(order);

		ResponseEntity<Map> response = cancel(orderId.toString(), "ACME-001");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("status")).isEqualTo("CANCELLED");
	}

	@Test
	void doubleCancelReturns409() {
		Map created = createOrder("ACME-001");
		String orderId = (String) created.get("id");
		assertThat(cancel(orderId, "ACME-001").getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<Map> secondAttempt = cancel(orderId, "ACME-001");

		assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(secondAttempt.getBody().get("currentStatus")).isEqualTo("CANCELLED");
	}

	@Test
	void cancelAfterFinalDeliveryReturns409() {
		Map created = createOrder("ACME-001");
		UUID orderId = UUID.fromString((String) created.get("id"));
		BulkOrder order = bulkOrderRepository.findById(orderId).orElseThrow();
		order.setStatus(OrderStatus.FINAL_DELIVERY);
		order.setNetTotalLockedAt(Instant.now());
		bulkOrderRepository.save(order);

		ResponseEntity<Map> response = cancel(orderId.toString(), "ACME-001");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().get("currentStatus")).isEqualTo("FINAL_DELIVERY");
	}

	@Test
	void cancelOfAnotherClientsOrderReturns404() {
		Map created = createOrder("ACME-001");
		String orderId = (String) created.get("id");

		ResponseEntity<Map> response = cancel(orderId, "GLOBEX-002");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
