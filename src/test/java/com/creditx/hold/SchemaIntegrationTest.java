package com.creditx.hold;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.flyway.locations=classpath:db/migration"
})
class SchemaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFlywayAppliedSchema() {
        // Test that CHS_PROCESSED_EVENTS table exists
        Integer processedEventsTableCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = 'CHS_PROCESSED_EVENTS'", Integer.class);
        assertThat(processedEventsTableCount).isEqualTo(1);

        // Test that CHS_HOLDS table exists
        Integer holdsTableCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = 'CHS_HOLDS'", Integer.class);
        assertThat(holdsTableCount).isEqualTo(1);

        // Test that CHS_OUTBOX_EVENTS table exists
        Integer outboxEventsTableCount = jdbcTemplate
                .queryForObject("SELECT COUNT(*) FROM user_tables WHERE table_name = 'CHS_OUTBOX_EVENTS'", Integer.class);
        assertThat(outboxEventsTableCount).isEqualTo(1);

        // Test inserting into CHS_PROCESSED_EVENTS
        jdbcTemplate.update("""
                    INSERT INTO CHS_PROCESSED_EVENTS (EVENT_ID, PAYLOAD_HASH, STATUS, PROCESSED_AT)
                    VALUES (?, ?, 'PROCESSED', SYSTIMESTAMP)
                """, "test-event-123", "hash123");

        Integer processedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM CHS_PROCESSED_EVENTS WHERE EVENT_ID = ?",
                Integer.class, "test-event-123");
        assertThat(processedCount).isEqualTo(1);

        // Test inserting into CHS_HOLDS
        jdbcTemplate.update("""
                    INSERT INTO CHS_HOLDS (TRANSACTION_ID, ACCOUNT_ID, AMOUNT, STATUS, EXPIRES_AT)
                    VALUES (?, ?, ?, 'AUTHORIZED', SYSTIMESTAMP + INTERVAL '7' DAY)
                """, 12345L, 1L, 100.50);

        Integer holdsCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM CHS_HOLDS WHERE TRANSACTION_ID = ?",
                Integer.class, 12345L);
        assertThat(holdsCount).isEqualTo(1);
    }
}