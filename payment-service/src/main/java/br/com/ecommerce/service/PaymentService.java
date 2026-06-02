package br.com.ecommerce.service;

import br.com.ecommerce.client.PaymentGatewayClient;
import br.com.ecommerce.domain.Payment;
import br.com.ecommerce.domain.PaymentMethod;
import br.com.ecommerce.domain.PaymentStatus;
import br.com.ecommerce.dto.*;
import br.com.ecommerce.messaging.PaymentEventPublisher;
import br.com.ecommerce.metrics.PaymentMetricsService;
import br.com.ecommerce.repository.PaymentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    PaymentRepository repository;

    @Inject
    PaymentEventPublisher publisher;

    @Inject
    PaymentMetricsService metricsService;

    @Inject
    @RestClient
    PaymentGatewayClient paymentGatewayClient;

    @Transactional
    public PaymentResponse process(PaymentRequestedEvent event) {
        validate(event);

        if (event.eventId() != null && repository.findByEventId(event.eventId()).isPresent()) {
            return PaymentResponse.fromEntity(repository.findByEventId(event.eventId()).orElseThrow());
        }

        Payment payment = new Payment();
        payment.eventId = event.eventId();
        payment.orderId = event.orderId();
        payment.userId = event.userId();
        payment.amount = event.amount();
        payment.currency = event.currency() == null ? "BRL" : event.currency();
        payment.paymentMethod = parsePaymentMethod(event.paymentMethod());
        payment.paymentToken = event.paymentToken();
        payment.installments = event.installments() == null ? 1 : event.installments();
        payment.status = PaymentStatus.REQUESTED;
        payment.createdAt = LocalDateTime.now();
        payment.updatedAt = LocalDateTime.now();

        repository.persist(payment);

        authorize(event, payment);

        return PaymentResponse.fromEntity(payment);
    }

    public List<PaymentResponse> listAll() {
        return repository.listAll()
                .stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    public PaymentResponse findById(Long id) {
        Payment payment = repository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado: " + id));

        return PaymentResponse.fromEntity(payment);
    }

    public PaymentResponse findByOrderId(Long orderId) {
        Payment payment = repository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Pagamento não encontrado para o pedido: " + orderId));

        return PaymentResponse.fromEntity(payment);
    }

    private void authorize(PaymentRequestedEvent event, Payment payment) {
        try {
            payment.attempts = payment.attempts + 1;
            payment.updatedAt = LocalDateTime.now();

            PaymentGatewayAuthorizeRequest request = new PaymentGatewayAuthorizeRequest(
                    payment.orderId,
                    payment.userId,
                    payment.amount,
                    payment.currency,
                    payment.paymentMethod.name(),
                    payment.paymentToken,
                    payment.installments
            );

            PaymentGatewayAuthorizeResponse response = paymentGatewayClient.authorize(request);

            payment.provider = response.provider();
            payment.providerTransactionId = response.transactionId();
            payment.authorizationCode = response.authorizationCode();
            payment.reason = response.reason();
            payment.updatedAt = LocalDateTime.now();

            if (response.approved()) {
                payment.status = PaymentStatus.APPROVED;
                payment.approvedAt = LocalDateTime.now();

                publisher.publishApproved(PaymentApprovedEvent.from(
                    event.correlationId(),
                    payment.orderId,
                    payment.userId,
                    payment.amount,
                    payment.providerTransactionId,
                    payment.authorizationCode,
                    payment.reason
                ));
            } else {
                payment.status = PaymentStatus.REJECTED;
                payment.rejectedAt = LocalDateTime.now();

                publisher.publishRejected(PaymentRejectedEvent.from(
                        event.correlationId(),
                        payment.orderId,
                        payment.userId,
                        payment.amount,
                        payment.providerTransactionId,
                        payment.reason
                ));
            }

            metricsService.recordPayment(payment.status, payment.provider, payment.amount);

        } catch (Exception exception) {
            payment.status = PaymentStatus.FAILED;
            payment.reason = "Falha ao consultar gateway de pagamento: " + exception.getMessage();
            payment.updatedAt = LocalDateTime.now();

            metricsService.recordPayment(payment.status, "PAYMENT_GATEWAY_ERROR", payment.amount);

            LOG.errorf(
                    exception,
                    "Falha ao processar pagamento. orderId=%s",
                    payment.orderId
            );
        }
    }

    private void validate(PaymentRequestedEvent event) {
        if (event == null) {
            throw new BadRequestException("Evento de pagamento inválido");
        }

        if (event.orderId() == null) {
            throw new BadRequestException("orderId é obrigatório");
        }

        if (event.userId() == null) {
            throw new BadRequestException("userId é obrigatório");
        }

        if (event.amount() == null || event.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount deve ser maior que zero");
        }
    }

    private PaymentMethod parsePaymentMethod(String value) {
        if (value == null || value.isBlank()) {
            return PaymentMethod.CREDIT_CARD;
        }

        return PaymentMethod.valueOf(value.trim().toUpperCase());
    }
}