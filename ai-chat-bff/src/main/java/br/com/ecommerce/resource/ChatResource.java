package br.com.ecommerce.resource;

import br.com.ecommerce.assistant.EcommerceAssistant;
import br.com.ecommerce.dto.ChatRequest;
import br.com.ecommerce.dto.ChatResponse;
import br.com.ecommerce.metrics.ChatMetricsService;
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

    private static final String LLM_MODEL = "llama3.2:latest";

    @Inject
    EcommerceAssistant assistant;

    @Inject
    ChatGuardrailService guardrailService;

    @Inject
    ChatIntentService intentService;

    @Inject
    EcommerceQueryService ecommerceQueryService;

    @Inject
    ChatMetricsService metricsService;

    @POST
    public ChatResponse chat(@Valid ChatRequest request) {
        ChatIntent intent = intentService.detectIntent(request.message());

        ChatGuardrailService.GuardrailResult guardrail = guardrailService.validate(request.message());

        if (!guardrail.allowed()) {
            metricsService.recordGuardrail(intent);

            return new ChatResponse(
                    guardrail.message(),
                    "guardrail",
                    LocalDateTime.now()
            );
        }

        LOG.infof("Mensagem recebida no ai-chat-bff. intent=%s, message=%s", intent, request.message());

        try {
            return switch (intent) {
                case LIST_PRODUCTS -> deterministicResponse(
                        ecommerceQueryService.listProducts(),
                        intent
                );

                case GET_PRODUCT -> intentService.extractFirstNumber(request.message())
                        .map(productId -> deterministicResponse(
                                ecommerceQueryService.getProduct(productId),
                                intent
                        ))
                        .orElseGet(() -> missingInfo("Informe o ID do produto que deseja consultar.", intent));

                case ESTIMATE_DELIVERY -> intentService.extractRoute(request.message())
                        .map(route -> deterministicResponse(
                                ecommerceQueryService.estimateDelivery(
                                        route.originState(),
                                        route.destinationState(),
                                        route.totalItems()
                                ),
                                intent
                        ))
                        .orElseGet(() -> missingInfo(
                                "Informe origem, destino e quantidade de itens. Exemplo: estime a entrega de 1 item saindo da BA para AL.",
                                intent
                        ));

                case GET_ORDER -> intentService.extractFirstNumber(request.message())
                        .map(orderId -> deterministicResponse(
                                ecommerceQueryService.getOrder(orderId),
                                intent
                        ))
                        .orElseGet(() -> missingInfo("Informe o ID do pedido que deseja consultar.", intent));

                case GENERAL_CHAT -> {
                    String answer = assistant.chat(request.message());
                    metricsService.recordLlm(intent, LLM_MODEL);

                    yield new ChatResponse(
                            answer,
                            LLM_MODEL,
                            LocalDateTime.now()
                    );
                }
            };
        } catch (Exception exception) {
            metricsService.recordError(intent);

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

    private ChatResponse deterministicResponse(String message, ChatIntent intent) {
        metricsService.recordDeterministicRouter(intent);

        return new ChatResponse(
                message,
                "deterministic-router",
                LocalDateTime.now()
        );
    }

    private ChatResponse missingInfo(String message, ChatIntent intent) {
        metricsService.recordDeterministicRouter(intent);

        return new ChatResponse(
                message,
                "deterministic-router",
                LocalDateTime.now()
        );
    }
}