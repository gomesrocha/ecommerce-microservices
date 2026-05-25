package br.com.ecommerce.service;

import br.com.ecommerce.domain.OutboxEvent;
import br.com.ecommerce.domain.OutboxStatus;
import br.com.ecommerce.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OutboxService {

    private static final int MAX_ATTEMPTS = 5;

    @Inject
    OutboxEventRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public void saveEvent(
            String aggregateType,
            Long aggregateId,
            String eventType,
            String routingKey,
            Object payload
    ) {
        OutboxEvent event = new OutboxEvent();

        event.eventId = UUID.randomUUID();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.routingKey = routingKey;
        event.payload = toJson(payload);
        event.status = OutboxStatus.PENDING;
        event.attempts = 0;

        repository.persist(event);
    }

    public List<OutboxEvent> listPending(int limit) {
        return repository.listPending(limit);
    }

    @Transactional
    public void markPublished(Long id) {
        OutboxEvent event = repository.findById(id);

        if (event == null) {
            return;
        }

        event.status = OutboxStatus.PUBLISHED;
        event.publishedAt = LocalDateTime.now();
        event.lastError = null;
    }

    @Transactional
    public void markFailed(Long id, Throwable throwable) {
        OutboxEvent event = repository.findById(id);

        if (event == null) {
            return;
        }

        event.attempts = event.attempts + 1;
        event.lastError = throwable.getMessage();

        if (event.attempts >= MAX_ATTEMPTS) {
            event.status = OutboxStatus.FAILED;
        }
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Falha ao serializar evento para outbox", exception);
        }
    }
}