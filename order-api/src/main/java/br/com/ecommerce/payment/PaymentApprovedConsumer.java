package br.com.ecommerce.payment;

import br.com.ecommerce.observability.CorrelationIdContext;
import br.com.ecommerce.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class PaymentApprovedConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentApprovedConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    OrderService orderService;

    @Incoming("payment-approved-in")
    public void consume(Buffer rawMessage) {
        String payload = rawMessage.toString(StandardCharsets.UTF_8);

        try {
            PaymentApprovedEvent event = objectMapper.readValue(payload, PaymentApprovedEvent.class);

            CorrelationIdContext.set(event.correlationId());

            orderService.approvePayment(
                    event.orderId(),
                    event.transactionId(),
                    event.authorizationCode(),
                    event.reason()
            );

            LOG.infof(
                    "PaymentApproved processado no order-api. orderId=%s, transactionId=%s, correlationId=%s",
                    event.orderId(),
                    event.transactionId(),
                    CorrelationIdContext.getOrCreate()
            );

        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao processar PaymentApproved. payload=%s",
                    payload
            );

            throw new IllegalStateException("Falha ao processar PaymentApproved", exception);

        } finally {
            CorrelationIdContext.clear();
        }
    }
}