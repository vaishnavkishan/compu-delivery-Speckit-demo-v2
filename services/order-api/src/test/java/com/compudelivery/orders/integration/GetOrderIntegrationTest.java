package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** FR-018: correct detail incl. transition history; identical generic 404. */
class GetOrderIntegrationTest extends IntegrationTestBase {

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private Map createOrder(String clientId) {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 3)));
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders(clientId)), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	@Test
	void returnsCorrectDetailIncludingTransitionHistory() {
		Map created = createOrder("ACME-001");
		String orderId = (String) created.get("id");

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId, HttpMethod.GET,
				new HttpEntity<>(clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("id")).isEqualTo(orderId);
		assertThat(response.getBody().get("status")).isEqualTo("INTAKE");
		List<Map<String, Object>> transitions = (List<Map<String, Object>>) response.getBody().get("transitions");
		assertThat(transitions).hasSize(1);
		assertThat(transitions.get(0).get("toStatus")).isEqualTo("INTAKE");
	}

	@Test
	void returnsIdenticalGenericNotFoundForNonexistentOrder() {
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + UUID.randomUUID(), HttpMethod.GET,
				new HttpEntity<>(clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void returnsIdenticalGenericNotFoundForAnotherClientsOrder() {
		Map created = createOrder("ACME-001");
		String orderId = (String) created.get("id");

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId, HttpMethod.GET,
				new HttpEntity<>(clientHeaders("GLOBEX-002")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
