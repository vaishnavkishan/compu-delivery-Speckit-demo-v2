package com.compudelivery.orders.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Computes Net Total per FR-003: Gross Total and the discount step are carried
 * at full precision, with rounding applied exactly once, at the final step,
 * using round-half-up (research.md #13).
 */
@Service
public class PricingService {

	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

	public BigDecimal netTotal(BigDecimal grossTotal, BigDecimal discountPercentage) {
		BigDecimal discountFraction = discountPercentage.divide(ONE_HUNDRED, MathContext.DECIMAL128);
		BigDecimal netTotalRaw = grossTotal.subtract(grossTotal.multiply(discountFraction));
		return netTotalRaw.setScale(2, RoundingMode.HALF_UP);
	}
}
