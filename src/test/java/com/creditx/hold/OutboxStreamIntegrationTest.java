package com.creditx.hold;

import com.creditx.hold.model.OutboxEvent;
import com.creditx.hold.model.OutboxEventStatus;
import com.creditx.hold.repository.OutboxEventRepository;
import com.creditx.hold.scheduler.OutboxEventPublishingScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@Import(TestChannelBinderConfiguration.class)
public class OutboxStreamIntegrationTest {

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

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublishingScheduler outboxEventPublisher;

    @Autowired
    private OutputDestination outputDestination;

    @Test
    @Transactional
    void testOutboxEventPublishedToStream() throws Exception {
        OutboxEvent outboxEvent = OutboxEvent.builder()
                .eventType("HOLD_AUTHORIZED")
                .aggregateId(123L)
                .payload("{\"HoldId\":\"hold-123\"}")
                .status(OutboxEventStatus.PENDING)
                .build();

        OutboxEvent savedEvent = outboxEventRepository.save(outboxEvent);
        assertThat(savedEvent.getEventId()).isNotNull();
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxEventStatus.PENDING);

        outboxEventPublisher.publishPendingEvents();

        OutboxEvent updatedEvent = outboxEventRepository.findById(savedEvent.getEventId()).orElseThrow();
        assertThat(updatedEvent.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(updatedEvent.getPublishedAt()).isNotNull();

        assertStreamMessageReceived("{\"HoldId\":\"hold-123\"}");
    }

    @Test
    @Transactional
    void testMultipleOutboxEventsPublishedToStream() throws Exception {
        OutboxEvent event1 = OutboxEvent.builder()
                .eventType("HOLD_AUTHORIZED")
                .aggregateId(100L)
                .payload("{\"HoldId\":\"hold-123\"}")
                .status(OutboxEventStatus.PENDING)
                .build();

        OutboxEvent event2 = OutboxEvent.builder()
                .eventType("HOLD_EXPIRED")
                .aggregateId(200L)
                .payload("{\"HoldId\":\"hold-456\"}")
                .status(OutboxEventStatus.PENDING)
                .build();

        outboxEventRepository.save(event1);
        outboxEventRepository.save(event2);

        outboxEventPublisher.publishPendingEvents();

        assertStreamMessageReceived("hold-123");
        assertStreamMessageReceived("hold-456");
    }

    private void assertStreamMessageReceived(String expectedContent) {
        Message<byte[]> received = outputDestination.receive(2000, "holds-out-0");

        assertThat(received).isNotNull();
        String messagePayload = new String(received.getPayload());
        assertThat(messagePayload).contains(expectedContent);
    }
}
