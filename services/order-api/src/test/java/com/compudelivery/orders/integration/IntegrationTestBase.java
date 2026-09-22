package com.compudelivery.orders.integration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Singleton-container pattern: containers are started once, statically, for the
 * whole test JVM (not per test class) and never explicitly stopped, avoiding
 * the start/stop churn that otherwise exhausts the local Docker VM's memory
 * across several integration test classes.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class IntegrationTestBase {

	static final PostgreSQLContainer<?> POSTGRES;
	static final RabbitMQContainer RABBITMQ;

	static {
		POSTGRES = new PostgreSQLContainer<>("postgres:17").withDatabaseName("order_api").withUsername("order_api")
				.withPassword("order_api");
		POSTGRES.start();
		RABBITMQ = new RabbitMQContainer("rabbitmq:3-management");
		RABBITMQ.start();
	}

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
		registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
	}

	@LocalServerPort
	protected int port;

	protected TestRestTemplate restTemplate = new TestRestTemplate();

	protected String baseUrl() {
		return "http://localhost:" + port + "/api";
	}
}
