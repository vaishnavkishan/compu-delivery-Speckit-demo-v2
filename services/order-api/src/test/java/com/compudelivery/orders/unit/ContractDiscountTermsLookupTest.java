package com.compudelivery.orders.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.compudelivery.orders.client.ContractDiscountTerms;
import com.compudelivery.orders.client.ContractDiscountTermsLookup;
import com.compudelivery.orders.client.ContractDiscountTermsRepository;
import com.compudelivery.orders.pricing.ContractTermsBlockedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FR-004: valid / missing / expired / ambiguous-overlap / out-of-(0,100)-range
 * terms.
 */
@ExtendWith(MockitoExtension.class)
class ContractDiscountTermsLookupTest {

	@Mock
	private ContractDiscountTermsRepository repository;

	private ContractDiscountTermsLookup lookup;
	private final Instant now = Instant.parse("2026-06-01T00:00:00Z");

	@BeforeEach
	void setUp() {
		lookup = new ContractDiscountTermsLookup(repository);
	}

	private ContractDiscountTerms terms(BigDecimal percentage, Instant from, Instant until) {
		ContractDiscountTerms terms = new ContractDiscountTerms();
		terms.setId(UUID.randomUUID());
		terms.setClientId("SOME-CLIENT");
		terms.setDiscountPercentage(percentage);
		terms.setEffectiveFrom(from);
		terms.setEffectiveUntil(until);
		terms.setCreatedAt(from);
		return terms;
	}

	@Test
	void resolvesTheCurrentlyEffectiveDiscountPercentage() {
		when(repository.findByClientId("ACME-001"))
				.thenReturn(List.of(terms(new BigDecimal("12.50"), now.minus(365, ChronoUnit.DAYS), null)));

		BigDecimal result = lookup.resolveCurrentDiscountPercentage("ACME-001", now);

		assertThat(result).isEqualTo(new BigDecimal("12.50"));
	}

	@Test
	void blocksWhenNoTermsExistAtAllForTheClient() {
		when(repository.findByClientId("NOTERMS-003")).thenReturn(List.of());

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("NOTERMS-003", now))
				.isInstanceOf(ContractTermsBlockedException.class).hasMessageContaining("missing");
	}

	@Test
	void blocksWhenTermsExistButNoneAreCurrentlyEffective() {
		when(repository.findByClientId("EXPIRED-004")).thenReturn(List
				.of(terms(new BigDecimal("10.00"), now.minus(365, ChronoUnit.DAYS), now.minus(30, ChronoUnit.DAYS))));

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("EXPIRED-004", now))
				.isInstanceOf(ContractTermsBlockedException.class).hasMessageContaining("expired");
	}

	@Test
	void blocksWhenMultipleTermsOverlapAtTheSameInstant() {
		when(repository.findByClientId("AMBIGUOUS-005"))
				.thenReturn(List.of(terms(new BigDecimal("5.00"), now.minus(365, ChronoUnit.DAYS), null),
						terms(new BigDecimal("15.00"), now.minus(180, ChronoUnit.DAYS), null)));

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("AMBIGUOUS-005", now))
				.isInstanceOf(ContractTermsBlockedException.class).hasMessageContaining("ambiguous");
	}

	@Test
	void blocksWhenDiscountPercentageIsZero() {
		when(repository.findByClientId("ZERO-CLIENT"))
				.thenReturn(List.of(terms(BigDecimal.ZERO, now.minus(1, ChronoUnit.DAYS), null)));

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("ZERO-CLIENT", now))
				.isInstanceOf(ContractTermsBlockedException.class);
	}

	@Test
	void blocksWhenDiscountPercentageIsAtOrAboveOneHundred() {
		when(repository.findByClientId("FULL-DISCOUNT"))
				.thenReturn(List.of(terms(new BigDecimal("100.00"), now.minus(1, ChronoUnit.DAYS), null)));

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("FULL-DISCOUNT", now))
				.isInstanceOf(ContractTermsBlockedException.class);
	}

	@Test
	void blocksWhenDiscountPercentageIsNegative() {
		when(repository.findByClientId("NEGATIVE-CLIENT"))
				.thenReturn(List.of(terms(new BigDecimal("-5.00"), now.minus(1, ChronoUnit.DAYS), null)));

		assertThatThrownBy(() -> lookup.resolveCurrentDiscountPercentage("NEGATIVE-CLIENT", now))
				.isInstanceOf(ContractTermsBlockedException.class);
	}
}
