package com.creditx.hold;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Instant;

@Testcontainers
@JdbcTest
@ActiveProfiles("test")
public class SchemaIntegrationTest {

    @SuppressWarnings("resource")
    @Container
    static final OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:latest-faststart")
            .withUsername("testuser")
            .withPassword("testpassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracle::getJdbcUrl);
        registry.add("spring.datasource.username", oracle::getUsername);
        registry.add("spring.datasource.password", oracle::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFlywayAppliedSchema() {
        Integer tableCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = 'CHS_HOLDS'", Integer.class);

        assertThat(tableCount).isEqualTo(1);

        Instant expiresAtInstant = Instant.now().plusSeconds(3600);
        Timestamp expiresAt = Timestamp.from(expiresAtInstant);

        jdbcTemplate.update("""
                    INSERT INTO CHS_HOLDS (TRANSACTION_ID, ACCOUNT_ID, AMOUNT, STATUS, EXPIRES_AT)
                    VALUES (1, 1, 100.25, 'AUTHORIZED', ?)
                """, expiresAt);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM CHS_HOLDS WHERE ACCOUNT_ID = ?",
                Integer.class, 1L);

        assertThat(count).isEqualTo(1);
    }
}