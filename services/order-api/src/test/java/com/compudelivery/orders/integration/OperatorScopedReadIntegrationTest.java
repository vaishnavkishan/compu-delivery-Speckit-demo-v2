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
 * FR-026: an operator supplying X-Operator-Id + X-Client-Id sees exactly what
 * that client would see, never widened beyond it.
 */
class OperatorScopedReadIntegrationTest extends IntegrationTestBase {

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private HttpHeaders operatorViewingClientHeaders(String operatorId, String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Operator-Id", operatorId);
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private Map createOrder(String clientId) {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 2)));
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders(clientId)), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	@Test
	void operatorListViewMatchesWhatTheClientWouldSee() {
		createOrder("ACME-001");

		ResponseEntity<Map> clientView = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.GET,
				new HttpEntity<>(clientHeaders("ACME-001")), Map.class);
		ResponseEntity<Map> operatorView = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.GET,
				new HttpEntity<>(operatorViewingClientHeaders("OPS-1", "ACME-001")), Map.class);

		assertThat(operatorView.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(operatorView.getBody().get("totalCount")).isEqualTo(clientView.getBody().get("totalCount"));
		assertThat(operatorView.getBody().get("items")).isEqualTo(clientView.getBody().get("items"));
	}

	@Test
	void operatorDetailViewMatchesWhatTheClientWouldSee() {
		Map created = createOrder("GLOBEX-002");
		String orderId = (String) created.get("id");

		ResponseEntity<Map> clientView = restTemplate.exchange(baseUrl() + "/orders/" + orderId, HttpMethod.GET,
				new HttpEntity<>(clientHeaders("GLOBEX-002")), Map.class);
		ResponseEntity<Map> operatorView = restTemplate.exchange(baseUrl() + "/orders/" + orderId, HttpMethod.GET,
				new HttpEntity<>(operatorViewingClientHeaders("OPS-1", "GLOBEX-002")), Map.class);

		assertThat(operatorView.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(operatorView.getBody()).isEqualTo(clientView.getBody());
	}

	@Test
	void operatorHeaderNeverWidensReadBeyondTheNamedClient() {
		Map created = createOrder("ACME-001");
		String orderId = (String) created.get("id");

		ResponseEntity<Map> operatorViewingDifferentClient = restTemplate.exchange(baseUrl() + "/orders/" + orderId,
				HttpMethod.GET, new HttpEntity<>(operatorViewingClientHeaders("OPS-1", "GLOBEX-002")), Map.class);

		assertThat(operatorViewingDifferentClient.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
