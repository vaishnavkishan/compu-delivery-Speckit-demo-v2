package com.compudelivery.orders.web;

import com.compudelivery.orders.identity.MissingIdentityException;
import com.compudelivery.orders.order.BulkOrder;
import com.compudelivery.orders.order.BulkOrderRepository;
import com.compudelivery.orders.order.InvalidLifecycleTransitionException;
import com.compudelivery.orders.order.InvalidOrderRequestException;
import com.compudelivery.orders.order.OrderConflictException;
import com.compudelivery.orders.order.OrderNotFoundException;
import com.compudelivery.orders.order.OrderStatus;
import com.compudelivery.orders.pricing.ContractTermsBlockedException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private final BulkOrderRepository bulkOrderRepository;

	public GlobalExceptionHandler(BulkOrderRepository bulkOrderRepository) {
		this.bulkOrderRepository = bulkOrderRepository;
	}

	@ExceptionHandler(MissingIdentityException.class)
	public ProblemDetail handleMissingIdentity(MissingIdentityException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(InvalidOrderRequestException.class)
	public ProblemDetail handleInvalidOrderRequest(InvalidOrderRequestException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getAllErrors().stream().findFirst()
				.map(error -> error.getDefaultMessage()).orElse("Invalid request");
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
	}

	@ExceptionHandler(ContractTermsBlockedException.class)
	public ProblemDetail handleContractTermsBlocked(ContractTermsBlockedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
	}

	@ExceptionHandler(OrderNotFoundException.class)
	public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(InvalidLifecycleTransitionException.class)
	public ConflictProblemDetail handleInvalidTransition(InvalidLifecycleTransitionException ex) {
		return new ConflictProblemDetail(ex.getMessage(), ex.getCurrentStatus());
	}

	@ExceptionHandler(OrderConflictException.class)
	public ConflictProblemDetail handleOrderConflict(OrderConflictException ex) {
		return new ConflictProblemDetail(ex.getMessage(), ex.getCurrentStatus());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ConflictProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
		// First-committed-wins (FR-016): the loser's transaction has already rolled
		// back, so
		// re-read the order's now-current status with a fresh query rather than reusing
		// any stale
		// in-memory copy (research.md #3).
		OrderStatus currentStatus = null;
		if (ex.getIdentifier() instanceof UUID orderId) {
			currentStatus = bulkOrderRepository.findById(orderId).map(BulkOrder::getStatus).orElse(null);
		}
		return new ConflictProblemDetail("The order was concurrently modified; please retry", currentStatus);
	}
}
