package br.com.ecommerce.messaging;

import br.com.ecommerce.dto.PaymentApprovedEvent;
import br.com.ecommerce.dto.PaymentRejectedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PaymentEventPublisher {

    private static final Logger LOG = Logger.getLogger(PaymentEventPublisher.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @Channel("payment-approved-out")
    Emitter<JsonObject> paymentApprovedEmitter;

    @Inject
    @Channel("payment-rejected-out")
    Emitter<JsonObject> paymentRejectedEmitter;

    public void publishApproved(PaymentApprovedEvent event) {
        JsonObject payload = toJsonObject(event);
        paymentApprovedEmitter.send(payload);

        LOG.infof(
                "Evento payment.approved publicado. orderId=%s, transactionId=%s",
                event.orderId(),
                event.transactionId()
        );
    }

    public void publishRejected(PaymentRejectedEvent event) {
        JsonObject payload = toJsonObject(event);
        paymentRejectedEmitter.send(payload);

        LOG.infof(
                "Evento payment.rejected publicado. orderId=%s, transactionId=%s",
                event.orderId(),
                event.transactionId()
        );
    }

    private JsonObject toJsonObject(Object event) {
        try {
            return new JsonObject(objectMapper.writeValueAsString(event));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao serializar evento de pagamento", exception);
        }
    }
}