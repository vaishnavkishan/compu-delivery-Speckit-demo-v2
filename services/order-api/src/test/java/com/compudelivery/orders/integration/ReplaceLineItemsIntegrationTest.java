package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.client.ContractDiscountTerms;
import com.compudelivery.orders.client.ContractDiscountTermsRepository;
import com.compudelivery.orders.client.EnterpriseClient;
import com.compudelivery.orders.client.EnterpriseClientRepository;
import com.compudelivery.orders.order.BulkOrder;
import com.compudelivery.orders.order.BulkOrderRepository;
import com.compudelivery.orders.order.OrderStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** FR-025, FR-017: line-item edit recalculation while Intake/Processing. */
class ReplaceLineItemsIntegrationTest extends IntegrationTestBase {

	@Autowired
	private BulkOrderRepository bulkOrderRepository;
	@Autowired
	private EnterpriseClientRepository enterpriseClientRepository;
	@Autowired
	private ContractDiscountTermsRepository contractDiscountTermsRepository;

	private HttpHeaders clientHeaders(String clientId) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Client-Id", clientId);
		return headers;
	}

	private Map createOrder(String clientId, List<Map<String, Object>> lineItems) {
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(Map.of("lineItems", lineItems), clientHeaders(clientId)), Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	@Test
	void recalculatesTotalsWhileIntake() {
		Map created = createOrder("ACME-001", List.of(Map.of("sku", "SKU-1001", "quantity", 10)));
		String orderId = (String) created.get("id");

		Instant start = Instant.now();
		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/line-items",
				HttpMethod.PUT,
				new HttpEntity<>(Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 20))),
						clientHeaders("ACME-001")),
				Map.class);
		Duration elapsed = Duration.between(start, Instant.now());

		// SC-001: the recalculated Gross/Net Total is returned within 5 seconds of the
		// edit.
		assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(new BigDecimal(response.getBody().get("grossTotal").toString()))
				.isEqualByComparingTo(new BigDecimal("84000.00"));
	}

	@Test
	void doesNotAutoMergeDuplicateSkuLineItems() {
		Map created = createOrder("ACME-001", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));
		String orderId = (String) created.get("id");

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/line-items",
				HttpMethod.PUT,
				new HttpEntity<>(Map.of("lineItems",
						List.of(Map.of("sku", "SKU-1001", "quantity", 5), Map.of("sku", "SKU-1001", "quantity", 3))),
						clientHeaders("ACME-001")),
				Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<?> lineItems = (List<?>) response.getBody().get("lineItems");
		assertThat(lineItems).hasSize(2);
		// 4200 * (5 + 3) = 33600.00
		assertThat(new BigDecimal(response.getBody().get("grossTotal").toString()))
				.isEqualByComparingTo(new BigDecimal("33600.00"));
	}

	@Test
	void rejectsEditPastProcessingLeavingExistingDataUnchanged() {
		Map created = createOrder("ACME-001", List.of(Map.of("sku", "SKU-1001", "quantity", 2)));
		UUID orderId = UUID.fromString((String) created.get("id"));

		BulkOrder order = bulkOrderRepository.findById(orderId).orElseThrow();
		order.setStatus(OrderStatus.SHIPPED);
		order.setNetTotalLockedAt(Instant.now());
		bulkOrderRepository.save(order);

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/line-items",
				HttpMethod.PUT,
				new HttpEntity<>(Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 99))),
						clientHeaders("ACME-001")),
				Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		BulkOrder unchanged = bulkOrderRepository.findById(orderId).orElseThrow();
		assertThat(unchanged.getGrossTotal()).isEqualByComparingTo(new BigDecimal("8400.00"));
	}

	@Test
	void rejectsEditWhenTermsHaveBecomeAmbiguousLeavingExistingTotalsUnchanged() {
		String clientId = "TEST-EDIT-AMBIG-" + UUID.randomUUID();
		EnterpriseClient client = new EnterpriseClient();
		client.setClientId(clientId);
		client.setDisplayName("Edit Ambiguity Test Client");
		client.setCreatedAt(Instant.now());
		enterpriseClientRepository.save(client);

		ContractDiscountTerms initialTerms = new ContractDiscountTerms();
		initialTerms.setId(UUID.randomUUID());
		initialTerms.setClientId(clientId);
		initialTerms.setDiscountPercentage(new BigDecimal("10.00"));
		initialTerms.setEffectiveFrom(Instant.now().minusSeconds(86400));
		initialTerms.setEffectiveUntil(null);
		initialTerms.setCreatedAt(Instant.now());
		contractDiscountTermsRepository.save(initialTerms);

		Map created = createOrder(clientId, List.of(Map.of("sku", "SKU-1001", "quantity", 2)));
		String orderId = (String) created.get("id");
		BigDecimal originalGrossTotal = new BigDecimal(created.get("grossTotal").toString());

		ContractDiscountTerms overlappingTerms = new ContractDiscountTerms();
		overlappingTerms.setId(UUID.randomUUID());
		overlappingTerms.setClientId(clientId);
		overlappingTerms.setDiscountPercentage(new BigDecimal("20.00"));
		overlappingTerms.setEffectiveFrom(Instant.now().minusSeconds(3600));
		overlappingTerms.setEffectiveUntil(null);
		overlappingTerms.setCreatedAt(Instant.now());
		contractDiscountTermsRepository.save(overlappingTerms);

		ResponseEntity<Map> response = restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/line-items",
				HttpMethod.PUT, new HttpEntity<>(Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 5))),
						clientHeaders(clientId)),
				Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		BulkOrder unchanged = bulkOrderRepository.findById(UUID.fromString(orderId)).orElseThrow();
		assertThat(unchanged.getGrossTotal()).isEqualByComparingTo(originalGrossTotal);
	}
}
