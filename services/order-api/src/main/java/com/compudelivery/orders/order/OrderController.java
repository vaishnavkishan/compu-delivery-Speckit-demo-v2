package com.compudelivery.orders.order;

import com.compudelivery.orders.identity.CallerIdentity;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderIntakeService orderIntakeService;
	private final OrderEditService orderEditService;
	private final OrderHistoryService orderHistoryService;
	private final OrderQueryService orderQueryService;
	private final OrderCancellationService orderCancellationService;
	private final OrderLifecycleAdvancementService orderLifecycleAdvancementService;

	public OrderController(OrderIntakeService orderIntakeService, OrderEditService orderEditService,
			OrderHistoryService orderHistoryService, OrderQueryService orderQueryService,
			OrderCancellationService orderCancellationService,
			OrderLifecycleAdvancementService orderLifecycleAdvancementService) {
		this.orderIntakeService = orderIntakeService;
		this.orderEditService = orderEditService;
		this.orderHistoryService = orderHistoryService;
		this.orderQueryService = orderQueryService;
		this.orderCancellationService = orderCancellationService;
		this.orderLifecycleAdvancementService = orderLifecycleAdvancementService;
	}

	@PostMapping
	@ApiResponse(responseCode = "201", description = "Order created in Intake status with calculated Net Total.")
	@ApiResponse(responseCode = "400", description = "Invalid line items (FR-014, FR-015, FR-030); no order is created.")
	@ApiResponse(responseCode = "422", description = "Contract discount terms are missing, ambiguous, or expired (FR-004).")
	public ResponseEntity<OrderDetailResponse> createOrder(CallerIdentity caller,
			@Valid @RequestBody CreateOrderRequest request) {
		OrderDetailResponse response = orderIntakeService.createOrder(caller.requireClientId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	@ApiResponse(responseCode = "200", description = "One newest-first page of only the named client's orders.")
	public OrderHistoryPageResponse listOrders(CallerIdentity caller,
			@RequestParam(name = "page", defaultValue = "0") int page) {
		return orderHistoryService.listOrders(caller.requireClientId(), page);
	}

	@GetMapping("/{orderId}")
	@ApiResponse(responseCode = "200", description = "OK")
	@ApiResponse(responseCode = "404", description = "Generic not-found, identical whether the order does not exist or belongs to a different client (FR-018).")
	public OrderDetailResponse getOrder(CallerIdentity caller, @PathVariable UUID orderId) {
		return orderQueryService.getOrder(caller.requireClientId(), orderId);
	}

	@PutMapping("/{orderId}/line-items")
	@ApiResponse(responseCode = "200", description = "Line items replaced; Gross Total and Net Total recalculated.")
	@ApiResponse(responseCode = "400", description = "Invalid line items (FR-014, FR-030); existing line items and totals are left unchanged.")
	@ApiResponse(responseCode = "422", description = "Contract discount terms are missing, ambiguous, or expired at the moment of the edit (FR-004, FR-025).")
	@ApiResponse(responseCode = "404", description = "Generic not-found (FR-018).")
	@ApiResponse(responseCode = "409", description = "Order is no longer editable or was concurrently modified (FR-016, FR-017, FR-025).")
	public OrderDetailResponse replaceLineItems(CallerIdentity caller, @PathVariable UUID orderId,
			@Valid @RequestBody ReplaceLineItemsRequest request) {
		return orderEditService.replaceLineItems(caller.requireClientId(), orderId, request);
	}

	@PostMapping("/{orderId}/cancel")
	@ApiResponse(responseCode = "200", description = "Order cancelled.")
	@ApiResponse(responseCode = "404", description = "Generic not-found (FR-018).")
	@ApiResponse(responseCode = "409", description = "Order already at Final Delivery, already Cancelled, or lost a first-committed-wins race (FR-011, FR-016).")
	public OrderDetailResponse cancelOrder(CallerIdentity caller, @PathVariable UUID orderId) {
		return orderCancellationService.cancel(caller.requireClientId(), orderId);
	}

	@PostMapping("/{orderId}/status")
	@ApiResponse(responseCode = "200", description = "Status advanced; transition recorded (FR-007).")
	@ApiResponse(responseCode = "404", description = "Order not found.")
	@ApiResponse(responseCode = "409", description = "Requested transition is not a valid next step, or lost a first-committed-wins race (FR-006, FR-016).")
	public OrderDetailResponse advanceStatus(CallerIdentity caller, @PathVariable UUID orderId,
			@Valid @RequestBody AdvanceStatusRequest request) {
		return orderLifecycleAdvancementService.advance(caller.requireOperatorId(), orderId, request.targetStatus());
	}
}
