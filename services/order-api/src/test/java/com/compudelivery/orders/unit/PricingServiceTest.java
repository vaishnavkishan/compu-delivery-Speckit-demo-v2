package com.compudelivery.orders.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.compudelivery.orders.pricing.PricingService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * FR-003: full-precision discount arithmetic, round-half-up only on the final
 * Net Total.
 */
class PricingServiceTest {

	private final PricingService pricingService = new PricingService();

	@Test
	void appliesDiscountAndRoundsOnlyTheFinalNetTotal() {
		BigDecimal grossTotal = new BigDecimal("22475.00");
		BigDecimal discountPercentage = new BigDecimal("12.5");

		BigDecimal netTotal = pricingService.netTotal(grossTotal, discountPercentage);

		assertThat(netTotal).isEqualTo(new BigDecimal("19665.63"));
	}

	@Test
	void roundsHalfUpAtExactlyTheMidpoint() {
		// 100 - 100 * (33/100) = 67.00 exactly; pick a case that lands on .xx5 before
		// rounding.
		BigDecimal grossTotal = new BigDecimal("100.00");
		BigDecimal discountPercentage = new BigDecimal("33.335");

		BigDecimal netTotal = pricingService.netTotal(grossTotal, discountPercentage);

		// 100 - 33.335 = 66.665 -> half-up -> 66.67
		assertThat(netTotal).isEqualTo(new BigDecimal("66.67"));
	}

	@Test
	void carriesFullPrecisionThroughTheDiscountStepBeforeRounding() {
		// A discount fraction that is non-terminating in decimal (1/3 %) must not be
		// truncated
		// before the final rounding step.
		BigDecimal grossTotal = new BigDecimal("300.00");
		BigDecimal discountPercentage = new BigDecimal("33.333333333333333333");

		BigDecimal netTotal = pricingService.netTotal(grossTotal, discountPercentage);

		assertThat(netTotal).isEqualTo(new BigDecimal("200.00"));
	}

	@Test
	void netTotalHasScaleOfExactlyTwo() {
		BigDecimal netTotal = pricingService.netTotal(new BigDecimal("50.00"), new BigDecimal("10"));

		assertThat(netTotal.scale()).isEqualTo(2);
		assertThat(netTotal).isEqualTo(new BigDecimal("45.00"));
	}
}
