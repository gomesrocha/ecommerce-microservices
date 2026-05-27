package br.com.ecommerce.consumer;

import br.com.ecommerce.dto.NotificationRequest;
import br.com.ecommerce.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.buffer.Buffer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class NotificationRequestedConsumer {

    private static final Logger LOG = Logger.getLogger(NotificationRequestedConsumer.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    NotificationService notificationService;

    @Incoming("notifications-requested-in")
    public void consume(Buffer rawMessage) {
        String payload = rawMessage.toString(StandardCharsets.UTF_8);

        try {
            NotificationRequest request = objectMapper.readValue(payload, NotificationRequest.class);
            notificationService.create(request);

            LOG.infof(
                    "NotificationRequest processado com sucesso. eventType=%s, userId=%s",
                    request.eventType(),
                    request.userId()
            );
        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao processar NotificationRequest. payload=%s",
                    payload
            );

            throw new IllegalStateException("Falha ao processar NotificationRequest", exception);
        }
    }
}