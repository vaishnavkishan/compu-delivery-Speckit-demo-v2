package com.compudelivery.orders.client;

import com.compudelivery.orders.pricing.ContractTermsBlockedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Resolves the "current" contract discount terms for a client at a given
 * instant, distinguishing missing/expired/ambiguous/out-of-range so the caller
 * can be told exactly which condition applied (FR-004).
 */
@Service
public class ContractDiscountTermsLookup {

	private final ContractDiscountTermsRepository repository;

	public ContractDiscountTermsLookup(ContractDiscountTermsRepository repository) {
		this.repository = repository;
	}

	public BigDecimal resolveCurrentDiscountPercentage(String clientId) {
		return resolveCurrentDiscountPercentage(clientId, Instant.now());
	}

	public BigDecimal resolveCurrentDiscountPercentage(String clientId, Instant at) {
		List<ContractDiscountTerms> all = repository.findByClientId(clientId);
		if (all.isEmpty()) {
			throw new ContractTermsBlockedException("Contract discount terms are missing for client " + clientId);
		}

		List<ContractDiscountTerms> current = all.stream().filter(t -> !t.getEffectiveFrom().isAfter(at))
				.filter(t -> t.getEffectiveUntil() == null || !t.getEffectiveUntil().isBefore(at)).toList();

		if (current.isEmpty()) {
			throw new ContractTermsBlockedException(
					"Contract discount terms have expired or are not yet effective for client " + clientId);
		}
		if (current.size() > 1) {
			throw new ContractTermsBlockedException(
					"Contract discount terms are ambiguous (multiple matching terms) for client " + clientId);
		}

		BigDecimal percentage = current.get(0).getDiscountPercentage();
		if (percentage.compareTo(BigDecimal.ZERO) <= 0 || percentage.compareTo(BigDecimal.valueOf(100)) >= 0) {
			throw new ContractTermsBlockedException("Contract discount percentage for client " + clientId
					+ " is not strictly between 0% and 100% and is treated as invalid");
		}
		return percentage;
	}
}
