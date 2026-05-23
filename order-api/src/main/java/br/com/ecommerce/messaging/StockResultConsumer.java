package br.com.ecommerce.messaging;

import br.com.ecommerce.event.StockRejectedEvent;
import br.com.ecommerce.event.StockReservedEvent;
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
public class StockResultConsumer {

    private static final Logger LOG = Logger.getLogger(StockResultConsumer.class);

    @Inject
    OrderService orderService;

    @Incoming("stock-reserved-in")
    @Blocking
    public CompletionStage<Void> consumeStockReserved(Message<JsonObject> message) {
        try {
            StockReservedEvent event = message.getPayload().mapTo(StockReservedEvent.class);

            LOG.infof("Evento StockReserved recebido. orderId=%s", event.payload().orderId());

            orderService.markStockReserved(
                    event.payload().orderId(),
                    event.payload().reason()
            );

            return message.ack();
        } catch (Exception exception) {
            LOG.error("Falha ao processar evento StockReserved", exception);
            return message.nack(exception);
        }
    }

    @Incoming("stock-rejected-in")
    @Blocking
    public CompletionStage<Void> consumeStockRejected(Message<JsonObject> message) {
        try {
            StockRejectedEvent event = message.getPayload().mapTo(StockRejectedEvent.class);

            LOG.infof("Evento StockRejected recebido. orderId=%s", event.payload().orderId());

            orderService.markStockRejected(
                    event.payload().orderId(),
                    event.payload().reason()
            );

            return message.ack();
        } catch (Exception exception) {
            LOG.error("Falha ao processar evento StockRejected", exception);
            return message.nack(exception);
        }
    }
}