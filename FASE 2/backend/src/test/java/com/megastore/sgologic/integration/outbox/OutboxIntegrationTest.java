package com.megastore.sgologic.integration.outbox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BB-04 contra infraestructura real: atomicidad negocio+evento, publicación
 * del sobre a RabbitMQ, concurrencia con {@code SKIP LOCKED} e idempotencia
 * del consumidor con PostgreSQL 17 y RabbitMQ 4 reales.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "sgo.outbox.poll-interval-ms=3600000")
class OutboxIntegrationTest {

    private static final String EXCHANGE = "sgo.events";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17.11-alpine3.23"))
            .withDatabaseName("sgo_logic_test")
            .withUsername("sgo_test")
            .withPassword("test_only_password");

    @Container
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:4.3.5-management-alpine"));

    @DynamicPropertySource
    static void infrastructureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ::getAdminPassword);
    }

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    PublishEvent publishEvent;

    @Autowired
    IdempotentConsumer idempotentConsumer;

    @Autowired
    OutboxDispatcher dispatcher;

    @Autowired
    OutboxEventRepository repository;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    ObjectMapper objectMapper;

    private TransactionTemplate transactionTemplate;
    private String captureQueue;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        repository.deleteAll();
        captureQueue = "it.outbox.capture." + UUID.randomUUID();
        amqpAdmin.declareQueue(QueueBuilder.durable(captureQueue).build());
        amqpAdmin.declareBinding(BindingBuilder.bind(new Queue(captureQueue))
                .to(new TopicExchange(EXCHANGE, true, false)).with("#"));
    }

    @AfterEach
    void tearDown() {
        amqpAdmin.deleteQueue(captureQueue);
    }

    @Test
    void eventIsAtomicWithBusinessTransaction() {
        UUID aggregateId = UUID.randomUUID();

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            publishEvent.publish(OutboxEventRequest.of("Order", aggregateId, "RollbackProbe",
                    UUID.randomUUID(), Map.of("probe", true)));
            throw new IllegalStateException("rollback deliberado");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(repository.countByAggregateId(aggregateId)).isZero();

        UUID eventId = transactionTemplate.execute(status -> publishEvent.publish(
                OutboxEventRequest.of("Order", aggregateId, "OrderCreated", UUID.randomUUID(), Map.of("probe", true))));

        assertThat(repository.countByAggregateId(aggregateId)).isEqualTo(1);
        assertThat(repository.findById(eventId).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    void publishWithoutActiveTransactionIsRejected() {
        assertThatThrownBy(() -> publishEvent.publish(OutboxEventRequest.of("Order", UUID.randomUUID(),
                "OrderCreated", UUID.randomUUID(), Map.of())))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void dispatcherPublishesCommonEnvelopeToBroker() {
        UUID correlationId = UUID.randomUUID();
        UUID aggregateId = UUID.randomUUID();
        UUID eventId = transactionTemplate.execute(status -> publishEvent.publish(new OutboxEventRequest(
                "Order", aggregateId, "OrderConfirmed", 1, correlationId, null,
                Map.of("orderId", aggregateId.toString()))));

        dispatcher.dispatchPending();

        assertThat(repository.findById(eventId).orElseThrow().getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        Message message = rabbitTemplate.receive(captureQueue, 5_000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());

        JsonNode envelope = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(envelope.get("eventId").asText()).isEqualTo(eventId.toString());
        assertThat(envelope.get("eventType").asText()).isEqualTo("OrderConfirmed");
        assertThat(envelope.get("eventVersion").asInt()).isEqualTo(1);
        assertThat(envelope.get("correlationId").asText()).isEqualTo(correlationId.toString());
        assertThat(envelope.get("aggregateId").asText()).isEqualTo(aggregateId.toString());
        assertThat(envelope.get("payload").get("orderId").asText()).isEqualTo(aggregateId.toString());
    }

    @Test
    void concurrentDispatchersPublishEachEventExactlyOnce() throws Exception {
        int total = 20;
        List<UUID> eventIds = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            for (int i = 0; i < total; i++) {
                eventIds.add(publishEvent.publish(OutboxEventRequest.of("Order", UUID.randomUUID(),
                        "OrderConfirmed", UUID.randomUUID(), Map.of("sequence", i))));
            }
        });

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = pool.submit(() -> dispatchAfter(start));
            Future<?> second = pool.submit(() -> dispatchAfter(start));
            start.countDown();
            first.get();
            second.get();
        } finally {
            pool.shutdown();
        }

        Set<String> receivedIds = new HashSet<>();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (receivedIds.size() < total && System.nanoTime() < deadline) {
            Message message = rabbitTemplate.receive(captureQueue, 2_000);
            if (message != null) {
                receivedIds.add(message.getMessageProperties().getMessageId());
            }
        }

        assertThat(receivedIds).containsExactlyInAnyOrderElementsOf(
                eventIds.stream().map(UUID::toString).toList());
        for (UUID eventId : eventIds) {
            assertThat(repository.findById(eventId).orElseThrow().getStatus())
                    .isEqualTo(OutboxEventStatus.PUBLISHED);
        }
    }

    @Test
    void inboxDeduplicatesRepeatedEventsPerConsumer() {
        UUID eventId = UUID.randomUUID();

        assertThat(idempotentConsumer.registerProcessed("it.consumer", eventId)).isTrue();
        assertThat(idempotentConsumer.registerProcessed("it.consumer", eventId)).isFalse();
        assertThat(idempotentConsumer.registerProcessed("it.other-consumer", eventId)).isTrue();
    }

    private void dispatchAfter(CountDownLatch start) {
        try {
            start.await();
            dispatcher.dispatchPending();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
