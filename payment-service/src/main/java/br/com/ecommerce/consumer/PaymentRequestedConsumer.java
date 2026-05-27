package br.com.ecommerce.consumer;

import br.com.ecommerce.dto.PaymentRequestedEvent;
import br.com.ecommerce.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class PaymentRequestedConsumer {

    private static final Logger LOG = Logger.getLogger(PaymentRequestedConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PaymentService paymentService;

    @Incoming("payment-requested-in")
    public void consume(Buffer rawMessage) {
        String payload = rawMessage.toString(StandardCharsets.UTF_8);

        try {
            PaymentRequestedEvent event = objectMapper.readValue(payload, PaymentRequestedEvent.class);
            paymentService.process(event);

            LOG.infof(
                    "PaymentRequested processado com sucesso. orderId=%s, amount=%s",
                    event.orderId(),
                    event.amount()
            );

        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao processar PaymentRequested. payload=%s",
                    payload
            );

            throw new IllegalStateException("Falha ao processar PaymentRequested", exception);
        }
    }
}