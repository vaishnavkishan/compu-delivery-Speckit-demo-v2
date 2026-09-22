package com.compudelivery.orders.order;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

public class MaxLineItemsValidator implements ConstraintValidator<MaxLineItems, List<?>> {

	private int max;

	@Override
	public void initialize(MaxLineItems constraintAnnotation) {
		this.max = constraintAnnotation.value();
	}

	@Override
	public boolean isValid(List<?> value, ConstraintValidatorContext context) {
		return value == null || value.size() <= max;
	}
}
