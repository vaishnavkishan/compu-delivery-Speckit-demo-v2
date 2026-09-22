package com.compudelivery.orders.identity;

import com.compudelivery.orders.client.EnterpriseClient;
import com.compudelivery.orders.client.EnterpriseClientRepository;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists the seeded demo client/operator identities the switcher lets the user
 * select (FR-019).
 */
@RestController
@RequestMapping("/api/demo-identities")
public class DemoIdentityController {

	private final EnterpriseClientRepository enterpriseClientRepository;
	private final OperatorRepository operatorRepository;

	public DemoIdentityController(EnterpriseClientRepository enterpriseClientRepository,
			OperatorRepository operatorRepository) {
		this.enterpriseClientRepository = enterpriseClientRepository;
		this.operatorRepository = operatorRepository;
	}

	@GetMapping
	public List<DemoIdentity> listDemoIdentities() {
		Stream<DemoIdentity> clients = enterpriseClientRepository.findAll().stream()
				.map((EnterpriseClient c) -> new DemoIdentity(c.getClientId(), c.getDisplayName(),
						DemoIdentity.Role.CLIENT, c.getContractReference()));
		Stream<DemoIdentity> operators = operatorRepository.findAll().stream()
				.map((Operator o) -> new DemoIdentity(o.getOperatorId(), o.getDisplayName(), DemoIdentity.Role.OPERATOR,
						null));
		return Stream.concat(clients, operators).toList();
	}
}
