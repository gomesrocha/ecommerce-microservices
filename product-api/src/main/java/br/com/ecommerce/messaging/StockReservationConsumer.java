package br.com.ecommerce.messaging;

import br.com.ecommerce.event.StockReservationRequestedEvent;
import br.com.ecommerce.service.ProductService;
import br.com.ecommerce.service.StockReservationResult;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletionStage;

@ApplicationScoped
public class StockReservationConsumer {

    private static final Logger LOG = Logger.getLogger(StockReservationConsumer.class);

    @Inject
    ProductService productService;

    @Inject
    StockEventPublisher stockEventPublisher;

    @Incoming("stock-reservation-in")
    @Blocking
    public CompletionStage<Void> consume(Message<JsonObject> message) {
        StockReservationRequestedEvent event = null;

        try {
            event = message.getPayload().mapTo(StockReservationRequestedEvent.class);

            LOG.infof("Evento StockReservationRequested recebido. orderId=%s", event.payload().orderId());

            StockReservationResult result = productService.reserveStock(
                    event.payload().orderId(),
                    event.payload().items()
            );

            if (result.reserved()) {
                stockEventPublisher.publishReserved(
                        event.payload().orderId(),
                        event.payload().userId(),
                        result.reason()
                );
            } else {
                stockEventPublisher.publishRejected(
                        event.payload().orderId(),
                        event.payload().userId(),
                        result.reason()
                );
            }

            return message.ack();
        } catch (Exception exception) {
            LOG.error("Falha ao reservar estoque", exception);

            if (event != null && event.payload() != null) {
                stockEventPublisher.publishRejected(
                        event.payload().orderId(),
                        event.payload().userId(),
                        exception.getMessage()
                );
            }

            return message.ack();
        }
    }
}