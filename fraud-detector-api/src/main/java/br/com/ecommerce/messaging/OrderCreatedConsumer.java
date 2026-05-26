package br.com.ecommerce.messaging;

import br.com.ecommerce.domain.FraudAnalysis;
import br.com.ecommerce.event.OrderCreatedEvent;
import br.com.ecommerce.service.FraudAnalysisService;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class);

    @Inject
    FraudAnalysisService fraudAnalysisService;

    @Inject
    FraudEventPublisher fraudEventPublisher;

    @Incoming("order-created-in")
    @Blocking
    public CompletionStage<Void> consume(Message<JsonObject> message) {
        try {
            OrderCreatedEvent event = message.getPayload().mapTo(OrderCreatedEvent.class);

            LOG.infof("Evento OrderCreated recebido. orderId=%s", event.payload().orderId());

            FraudAnalysis analysis = fraudAnalysisService.analyze(event);

            fraudEventPublisher.publish(analysis);

            return message.ack();
        } catch (Exception exception) {
            LOG.error("Falha ao processar evento OrderCreated", exception);
            return message.nack(exception);
        }
    }
}