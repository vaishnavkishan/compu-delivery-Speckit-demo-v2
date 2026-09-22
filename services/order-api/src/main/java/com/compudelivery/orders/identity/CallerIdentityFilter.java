package com.compudelivery.orders.identity;

import com.compudelivery.orders.client.EnterpriseClientRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves the trusted, caller-supplied
 * {@code X-Client-Id}/{@code X-Operator-Id} identity for every request
 * (research.md #1). Rejects with 400 as soon as a supplied header does not
 * match a seeded identity; leaves per-endpoint requiredness to the
 * controller/service layer via
 * {@link CallerIdentity#requireClientId()}/{@link CallerIdentity#requireOperatorId()}.
 */
@Component
public class CallerIdentityFilter extends OncePerRequestFilter {

	public static final String REQUEST_ATTRIBUTE = "callerIdentity";

	private final EnterpriseClientRepository enterpriseClientRepository;
	private final OperatorRepository operatorRepository;
	private final ObjectMapper objectMapper;

	public CallerIdentityFilter(EnterpriseClientRepository enterpriseClientRepository,
			OperatorRepository operatorRepository, ObjectMapper objectMapper) {
		this.enterpriseClientRepository = enterpriseClientRepository;
		this.operatorRepository = operatorRepository;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String clientId = request.getHeader("X-Client-Id");
		String operatorId = request.getHeader("X-Operator-Id");

		if (clientId != null && enterpriseClientRepository.findById(clientId).isEmpty()) {
			writeUnknownIdentity(response, "X-Client-Id", clientId);
			return;
		}
		if (operatorId != null && operatorRepository.findById(operatorId).isEmpty()) {
			writeUnknownIdentity(response, "X-Operator-Id", operatorId);
			return;
		}

		request.setAttribute(REQUEST_ATTRIBUTE, new CallerIdentity(clientId, operatorId));
		filterChain.doFilter(request, response);
	}

	private void writeUnknownIdentity(HttpServletResponse response, String header, String value) throws IOException {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				header + " '" + value + "' does not match a known identity");
		response.setStatus(HttpStatus.BAD_REQUEST.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), problem);
	}
}
