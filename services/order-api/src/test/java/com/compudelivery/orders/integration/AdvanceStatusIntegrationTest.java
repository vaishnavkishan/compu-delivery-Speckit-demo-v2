package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * FR-006, FR-012: full forward progression, rejected skip, rejected change
 * after a terminal state, and the Processing<->Backordered reversal.
 */
class AdvanceStatusIntegrationTest extends IntegrationTestBase {

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private HttpHeaders operatorHeaders(String operatorId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Operator-Id", operatorId);
		return headers;
	}

	private String createOrder() {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return (String) response.getBody().get("id");
	}

	private ResponseEntity<Map> advance(String orderId, String targetStatus) {
		return restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/status", HttpMethod.POST,
				new HttpEntity<>(Map.of("targetStatus", targetStatus), operatorHeaders("OPS-1")), Map.class);
	}

	@Test
	void fullForwardProgressionSucceedsAtEachStep() {
		String orderId = createOrder();

		ResponseEntity<Map> toProcessing = advance(orderId, "PROCESSING");
		assertThat(toProcessing.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(toProcessing.getBody().get("status")).isEqualTo("PROCESSING");

		ResponseEntity<Map> toShipped = advance(orderId, "SHIPPED");
		assertThat(toShipped.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(toShipped.getBody().get("status")).isEqualTo("SHIPPED");
		assertThat(toShipped.getBody().get("netTotalLocked")).isEqualTo(true);

		ResponseEntity<Map> toFinalDelivery = advance(orderId, "FINAL_DELIVERY");
		assertThat(toFinalDelivery.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(toFinalDelivery.getBody().get("status")).isEqualTo("FINAL_DELIVERY");
	}

	@Test
	void rejectsSkippingAStage() {
		String orderId = createOrder();

		ResponseEntity<Map> response = advance(orderId, "SHIPPED");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().get("currentStatus")).isEqualTo("INTAKE");
	}

	@Test
	void rejectsChangeAfterTerminalState() {
		String orderId = createOrder();
		advance(orderId, "PROCESSING");
		advance(orderId, "SHIPPED");
		advance(orderId, "FINAL_DELIVERY");

		ResponseEntity<Map> response = advance(orderId, "PROCESSING");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(response.getBody().get("currentStatus")).isEqualTo("FINAL_DELIVERY");
	}

	@Test
	void processingToBackorderedAndBackSucceedsBothWays() {
		String orderId = createOrder();
		advance(orderId, "PROCESSING");

		ResponseEntity<Map> toBackordered = advance(orderId, "BACKORDERED");
		assertThat(toBackordered.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(toBackordered.getBody().get("status")).isEqualTo("BACKORDERED");

		ResponseEntity<Map> backToProcessing = advance(orderId, "PROCESSING");
		assertThat(backToProcessing.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(backToProcessing.getBody().get("status")).isEqualTo("PROCESSING");
	}
}
