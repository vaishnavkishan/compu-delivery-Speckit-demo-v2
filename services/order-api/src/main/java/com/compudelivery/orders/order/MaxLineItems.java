package com.compudelivery.orders.order;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rejects an order/edit containing more than {@link #value()} line items
 * (FR-030).
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxLineItemsValidator.class)
public @interface MaxLineItems {

	int value() default 100;

	String message() default "Order cannot contain more than {value} line items";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
