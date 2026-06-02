package br.com.ecommerce.messaging;

import br.com.ecommerce.domain.FraudAnalysis;
import br.com.ecommerce.domain.FraudStatus;
import br.com.ecommerce.event.FraudApprovedEvent;
import br.com.ecommerce.event.FraudRejectedEvent;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class FraudEventPublisher {

    private static final Logger LOG = Logger.getLogger(FraudEventPublisher.class);

    @Inject
    @Channel("fraud-approved-out")
    Emitter<FraudApprovedEvent> fraudApprovedEmitter;

    @Inject
    @Channel("fraud-rejected-out")
    Emitter<FraudRejectedEvent> fraudRejectedEmitter;

    public void publish(String correlationId, FraudAnalysis analysis) {
        if (FraudStatus.APPROVED.equals(analysis.status)) {
            publishApproved(correlationId, analysis);
            return;
        }

        publishRejected(correlationId, analysis);
    }

    private void publishApproved(String correlationId, FraudAnalysis analysis) {
        FraudApprovedEvent event = FraudApprovedEvent.fromEntity(correlationId, analysis);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("fraud.approved")
                .withTimestamp(ZonedDateTime.now())
                .withHeader("eventType", "FraudApproved")
                .withHeader("sourceService", "fraud-detector-api")
                .withHeader("correlationId", correlationId)
                .build();

        Message<FraudApprovedEvent> message = Message.of(
                event,
                () -> {
                    LOG.infof(
                            "Evento FraudApproved publicado para pedido %s. correlationId=%s",
                            analysis.orderId,
                            correlationId
                    );
                    return CompletableFuture.completedFuture(null);
                },
                throwable -> {
                    LOG.errorf(
                            throwable,
                            "Falha ao publicar FraudApproved para pedido %s. correlationId=%s",
                            analysis.orderId,
                            correlationId
                    );
                    return CompletableFuture.completedFuture(null);
                }
        ).addMetadata(metadata);

        fraudApprovedEmitter.send(message);
    }

    private void publishRejected(String correlationId, FraudAnalysis analysis) {
        FraudRejectedEvent event = FraudRejectedEvent.fromEntity(correlationId, analysis);

        OutgoingRabbitMQMetadata metadata = new OutgoingRabbitMQMetadata.Builder()
                .withRoutingKey("fraud.rejected")
                .withTimestamp(ZonedDateTime.now())
                .withHeader("eventType", "FraudRejected")
                .withHeader("sourceService", "fraud-detector-api")
                .withHeader("correlationId", correlationId)
                .build();

        Message<FraudRejectedEvent> message = Message.of(
                event,
                () -> {
                    LOG.infof(
                            "Evento FraudRejected publicado para pedido %s. correlationId=%s",
                            analysis.orderId,
                            correlationId
                    );
                    return CompletableFuture.completedFuture(null);
                },
                throwable -> {
                    LOG.errorf(
                            throwable,
                            "Falha ao publicar FraudRejected para pedido %s. correlationId=%s",
                            analysis.orderId,
                            correlationId
                    );
                    return CompletableFuture.completedFuture(null);
                }
        ).addMetadata(metadata);

        fraudRejectedEmitter.send(message);
    }
}