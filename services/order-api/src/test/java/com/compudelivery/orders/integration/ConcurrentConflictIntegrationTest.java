package com.compudelivery.orders.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.order.BulkOrder;
import com.compudelivery.orders.order.BulkOrderRepository;
import com.compudelivery.orders.order.OrderStatus;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * FR-016 first-committed-wins: a concurrent cancel and an operator
 * advance-to-Final-Delivery on the same order resolve with exactly one success
 * and one rejection carrying the order's now-current status.
 */
class ConcurrentConflictIntegrationTest extends IntegrationTestBase {

	@Autowired
	private BulkOrderRepository bulkOrderRepository;

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

	@Test
	void exactlyOneOfCancelAndAdvanceSucceeds() throws Exception {
		Map<String, Object> body = Map.of("lineItems", List.of(Map.of("sku", "SKU-1001", "quantity", 1)));
		ResponseEntity<Map> created = restTemplate.exchange(baseUrl() + "/orders", HttpMethod.POST,
				new HttpEntity<>(body, clientHeaders("ACME-001")), Map.class);
		UUID orderId = UUID.fromString((String) created.getBody().get("id"));

		BulkOrder order = bulkOrderRepository.findById(orderId).orElseThrow();
		order.setStatus(OrderStatus.SHIPPED);
		bulkOrderRepository.save(order);

		CyclicBarrier barrier = new CyclicBarrier(2);

		CompletableFuture<ResponseEntity<Map>> cancelFuture = CompletableFuture.supplyAsync(() -> {
			awaitQuietly(barrier);
			return restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/cancel", HttpMethod.POST,
					new HttpEntity<>(clientHeaders("ACME-001")), Map.class);
		});

		CompletableFuture<ResponseEntity<Map>> advanceFuture = CompletableFuture.supplyAsync(() -> {
			awaitQuietly(barrier);
			return restTemplate.exchange(baseUrl() + "/orders/" + orderId + "/status", HttpMethod.POST,
					new HttpEntity<>(Map.of("targetStatus", "FINAL_DELIVERY"), operatorHeaders("OPS-1")), Map.class);
		});

		ResponseEntity<Map> cancelResponse = cancelFuture.get();
		ResponseEntity<Map> advanceResponse = advanceFuture.get();

		List<ResponseEntity<Map>> responses = List.of(cancelResponse, advanceResponse);
		long successCount = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.OK).count();
		long conflictCount = responses.stream().filter(r -> r.getStatusCode() == HttpStatus.CONFLICT).count();

		assertThat(successCount).isEqualTo(1);
		assertThat(conflictCount).isEqualTo(1);

		ResponseEntity<Map> conflictResponse = cancelResponse.getStatusCode() == HttpStatus.CONFLICT
				? cancelResponse
				: advanceResponse;
		BulkOrder finalOrder = bulkOrderRepository.findById(orderId).orElseThrow();
		assertThat(conflictResponse.getBody().get("currentStatus")).isEqualTo(finalOrder.getStatus().name());
	}

	private void awaitQuietly(CyclicBarrier barrier) {
		try {
			barrier.await();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
