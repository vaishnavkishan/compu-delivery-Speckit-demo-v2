package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.order.BulkOrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** FR-001–FR-004, FR-014, FR-015, FR-030. */
class CreateOrderIntegrationTest extends IntegrationTestBase {

	@Autowired
	private BulkOrderRepository bulkOrderRepository;

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	@Test
	void happyPathReturns201WithCorrectNetTotal() {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 25)));

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().get("status")).isEqualTo("INTAKE");
		// 25 * 4200.00 = 105000.00 gross; ACME-001's seeded discount is 12.5%.
		assertThat(new BigDecimal(response.getBody().get("grossTotal").toString()))
				.isEqualByComparingTo(new BigDecimal("105000.00"));
		assertThat(new BigDecimal(response.getBody().get("netTotal").toString()))
				.isEqualByComparingTo(new BigDecimal("91875.00"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"NOTERMS-003", "EXPIRED-004", "AMBIGUOUS-005"})
	void blockedTermsReturn422AndPersistNoOrder(String clientId) {
		long countBefore = bulkOrderRepository.count();
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders(clientId)), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(bulkOrderRepository.count()).isEqualTo(countBefore);
	}

	@Test
	void quantityAboveCeilingReturns400() {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 10001)));

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void moreThanOneHundredLineItemsReturns400() {
		List<Map<String, Object>> lineItems = java.util.stream.IntStream.range(0, 101)
				.mapToObj(i -> Map.<String, Object>of("sku", "SKU-1001", "quantity", 1)).toList();
		Map<String, Object> body = Map.of("lineItems", lineItems);

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void emptyOrderReturns400() {
		Map<String, Object> body = Map.of("lineItems", List.of());

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void unknownSkuReturns400() {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-NOT-REAL", "quantity", 1)));

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders",
				org.springframework.http.HttpMethod.POST, new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
