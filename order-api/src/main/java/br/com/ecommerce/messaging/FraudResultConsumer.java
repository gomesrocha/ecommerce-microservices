package br.com.ecommerce.messaging;

import br.com.ecommerce.event.FraudApprovedEvent;
import br.com.ecommerce.event.FraudRejectedEvent;
import br.com.ecommerce.observability.CorrelationIdContext;
import br.com.ecommerce.service.OrderService;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class FraudResultConsumer {

    private static final Logger LOG = Logger.getLogger(FraudResultConsumer.class);

    @Inject
    OrderService orderService;

    @Incoming("fraud-approved-in")
    @Blocking
    public CompletionStage<Void> consumeFraudApproved(Message<JsonObject> message) {
        try {
            FraudApprovedEvent event = message.getPayload().mapTo(FraudApprovedEvent.class);

            CorrelationIdContext.set(event.correlationId());

            LOG.infof(
                    "Evento FraudApproved recebido. orderId=%s, correlationId=%s",
                    event.payload().orderId(),
                    CorrelationIdContext.getOrCreate()
            );

            orderService.approveFraud(
                    event.payload().orderId(),
                    event.payload().riskScore(),
                    event.payload().reason()
            );

            return message.ack();

        } catch (Exception exception) {
            LOG.error("Falha ao processar evento FraudApproved", exception);
            return message.nack(exception);

        } finally {
            CorrelationIdContext.clear();
        }
    }

    @Incoming("fraud-rejected-in")
    @Blocking
    public CompletionStage<Void> consumeFraudRejected(Message<JsonObject> message) {
        try {
            FraudRejectedEvent event = message.getPayload().mapTo(FraudRejectedEvent.class);

            CorrelationIdContext.set(event.correlationId());

            LOG.infof(
                    "Evento FraudRejected recebido. orderId=%s, correlationId=%s",
                    event.payload().orderId(),
                    CorrelationIdContext.getOrCreate()
            );

            orderService.rejectFraud(
                    event.payload().orderId(),
                    event.payload().riskScore(),
                    event.payload().reason()
            );

            return message.ack();

        } catch (Exception exception) {
            LOG.error("Falha ao processar evento FraudRejected", exception);
            return message.nack(exception);

        } finally {
            CorrelationIdContext.clear();
        }
    }
}