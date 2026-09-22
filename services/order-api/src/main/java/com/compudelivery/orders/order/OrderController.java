package com.compudelivery.orders.order;

import com.compudelivery.orders.identity.CallerIdentity;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	private final OrderIntakeService orderIntakeService;
	private final OrderEditService orderEditService;

	public OrderController(OrderIntakeService orderIntakeService, OrderEditService orderEditService) {
		this.orderIntakeService = orderIntakeService;
		this.orderEditService = orderEditService;
	}

	@PostMapping
	public ResponseEntity<OrderDetailResponse> createOrder(CallerIdentity caller,
			@Valid @RequestBody CreateOrderRequest request) {
		OrderDetailResponse response = orderIntakeService.createOrder(caller.requireClientId(), request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PutMapping("/{orderId}/line-items")
	public OrderDetailResponse replaceLineItems(CallerIdentity caller, @PathVariable UUID orderId,
			@Valid @RequestBody ReplaceLineItemsRequest request) {
		return orderEditService.replaceLineItems(caller.requireClientId(), orderId, request);
	}
}
