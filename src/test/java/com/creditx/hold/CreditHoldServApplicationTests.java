package com.creditx.hold;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestChannelBinderConfiguration.class)
class CreditHoldServApplicationTests {

	@SuppressWarnings("resource")
	@Container
	static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:latest-faststart")
			.withUsername("testuser")
			.withPassword("testpass");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", oracle::getJdbcUrl);
		registry.add("spring.datasource.username", oracle::getUsername);
		registry.add("spring.datasource.password", oracle::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
	}

	@Test
	void contextLoads() {
	}

}
