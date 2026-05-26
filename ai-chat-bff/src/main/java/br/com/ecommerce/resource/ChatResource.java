package br.com.ecommerce.resource;

import br.com.ecommerce.assistant.EcommerceAssistant;
import br.com.ecommerce.dto.ChatRequest;
import br.com.ecommerce.dto.ChatResponse;
import br.com.ecommerce.service.ChatGuardrailService;
import br.com.ecommerce.service.ChatIntent;
import br.com.ecommerce.service.ChatIntentService;
import br.com.ecommerce.service.EcommerceQueryService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;

@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);

    @Inject
    EcommerceAssistant assistant;

    @Inject
    ChatGuardrailService guardrailService;

    @Inject
    ChatIntentService intentService;

    @Inject
    EcommerceQueryService ecommerceQueryService;

    @POST
    public ChatResponse chat(@Valid ChatRequest request) {
        ChatGuardrailService.GuardrailResult guardrail = guardrailService.validate(request.message());

        if (!guardrail.allowed()) {
            return new ChatResponse(
                    guardrail.message(),
                    "guardrail",
                    LocalDateTime.now()
            );
        }

        ChatIntent intent = intentService.detectIntent(request.message());

        LOG.infof("Mensagem recebida no ai-chat-bff. intent=%s, message=%s", intent, request.message());

        try {
            return switch (intent) {
                case LIST_PRODUCTS -> new ChatResponse(
                        ecommerceQueryService.listProducts(),
                        "deterministic-router",
                        LocalDateTime.now()
                );

                case GET_PRODUCT -> intentService.extractFirstNumber(request.message())
                        .map(productId -> new ChatResponse(
                                ecommerceQueryService.getProduct(productId),
                                "deterministic-router",
                                LocalDateTime.now()
                        ))
                        .orElseGet(() -> missingInfo("Informe o ID do produto que deseja consultar."));

                case ESTIMATE_DELIVERY -> intentService.extractRoute(request.message())
                        .map(route -> new ChatResponse(
                                ecommerceQueryService.estimateDelivery(
                                        route.originState(),
                                        route.destinationState(),
                                        route.totalItems()
                                ),
                                "deterministic-router",
                                LocalDateTime.now()
                        ))
                        .orElseGet(() -> missingInfo(
                                "Informe origem, destino e quantidade de itens. Exemplo: estime a entrega de 1 item saindo da BA para AL."
                        ));

                case GET_ORDER -> intentService.extractFirstNumber(request.message())
                        .map(orderId -> new ChatResponse(
                                ecommerceQueryService.getOrder(orderId),
                                "deterministic-router",
                                LocalDateTime.now()
                        ))
                        .orElseGet(() -> missingInfo("Informe o ID do pedido que deseja consultar."));

                case GENERAL_CHAT -> new ChatResponse(
                        assistant.chat(request.message()),
                        "llama3.2:latest",
                        LocalDateTime.now()
                );
            };
        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Erro ao processar mensagem no ai-chat-bff. intent=%s, message=%s",
                    intent,
                    request.message()
            );

            return new ChatResponse(
                    "Não consegui consultar essa informação agora. Verifique se os serviços internos estão disponíveis e tente novamente.",
                    "error-handler",
                    LocalDateTime.now()
            );
        }
    }

    @GET
    @Path("/health")
    public String health() {
        return "ai-chat-bff UP";
    }

    private ChatResponse missingInfo(String message) {
        return new ChatResponse(
                message,
                "deterministic-router",
                LocalDateTime.now()
        );
    }
}