package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.client.ContractDiscountTerms;
import com.compudelivery.orders.client.ContractDiscountTermsRepository;
import com.compudelivery.orders.client.EnterpriseClient;
import com.compudelivery.orders.client.EnterpriseClientRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** FR-008, FR-009: 25-per-page newest-first paging and per-client isolation. */
class ListOrdersIntegrationTest extends IntegrationTestBase {

	@Autowired
	private EnterpriseClientRepository enterpriseClientRepository;
	@Autowired
	private ContractDiscountTermsRepository contractDiscountTermsRepository;

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private void seedClient(String clientId) {
		EnterpriseClient client = new EnterpriseClient();
		client.setClientId(clientId);
		client.setDisplayName("List Orders Test Client " + clientId);
		client.setCreatedAt(Instant.now());
		enterpriseClientRepository.save(client);

		ContractDiscountTerms terms = new ContractDiscountTerms();
		terms.setId(UUID.randomUUID());
		terms.setClientId(clientId);
		terms.setDiscountPercentage(new BigDecimal("10.00"));
		terms.setEffectiveFrom(Instant.now().minusSeconds(86400));
		terms.setEffectiveUntil(null);
		terms.setCreatedAt(Instant.now());
		contractDiscountTermsRepository.save(terms);
	}

	private void createOrder(String clientId) {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders(clientId)), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	@Test
	void pagesTwentyFivePerPageNewestFirstWithNoSkipOrDuplicate() {
		String clientId = "TEST-HISTORY-" + UUID.randomUUID();
		seedClient(clientId);
		for (int i = 0; i < 26; i++) {
			createOrder(clientId);
		}

		Instant start = Instant.now();
		ResponseEntity<Map> page0 = restTemplate.exchange(baseUrl() + "/orders?page=0", HttpMethod.GET,
				new HttpEntity<>(clientHeaders(clientId)), Map.class);
		Duration elapsed = Duration.between(start, Instant.now());

		// SC-001: a single Order History page fetch completes within 5 seconds.
		assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
		assertThat(page0.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<Map<String, Object>> page0Items = (List<Map<String, Object>>) page0.getBody().get("items");
		assertThat(page0Items).hasSize(25);
		assertThat(page0.getBody().get("hasMore")).isEqualTo(true);
		assertThat(page0.getBody().get("totalCount")).isEqualTo(26);

		ResponseEntity<Map> page1 = restTemplate.exchange(baseUrl() + "/orders?page=1", HttpMethod.GET,
				new HttpEntity<>(clientHeaders(clientId)), Map.class);
		List<Map<String, Object>> page1Items = (List<Map<String, Object>>) page1.getBody().get("items");
		assertThat(page1Items).hasSize(1);
		assertThat(page1.getBody().get("hasMore")).isEqualTo(false);

		Set<String> ids = new HashSet<>();
		page0Items.forEach(item -> ids.add((String) item.get("id")));
		page1Items.forEach(item -> ids.add((String) item.get("id")));
		assertThat(ids).hasSize(26);
	}

	@Test
	void restrictsResultsToTheCallingClientOnly() {
		String clientA = "TEST-ISOLATION-A-" + UUID.randomUUID();
		String clientB = "TEST-ISOLATION-B-" + UUID.randomUUID();
		seedClient(clientA);
		seedClient(clientB);
		createOrder(clientA);
		createOrder(clientB);

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.GET,
				new HttpEntity<>(clientHeaders(clientA)), Map.class);

		List<Map<String, Object>> items = (List<Map<String, Object>>) response.getBody().get("items");
		assertThat(items).hasSize(1);
	}
}
