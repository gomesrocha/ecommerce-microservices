package br.com.ecommerce.service;

import br.com.ecommerce.domain.FraudAnalysis;
import br.com.ecommerce.domain.FraudStatus;
import br.com.ecommerce.event.OrderCreatedEvent;
import br.com.ecommerce.ml.FraudPredictionResult;
import br.com.ecommerce.ml.FraudTribuoModelService;
import br.com.ecommerce.repository.FraudAnalysisRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import br.com.ecommerce.metrics.FraudMetricsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class FraudAnalysisService {

    private static final BigDecimal RULE_REJECTION_THRESHOLD = new BigDecimal("70.00");

    @Inject
    FraudAnalysisRepository repository;

    @Inject
    FraudTribuoModelService fraudTribuoModelService;

    @Inject
    FraudMetricsService metricsService;

    @Transactional
    public FraudAnalysis analyze(OrderCreatedEvent event) {
        validateEvent(event);

        Long orderId = event.payload().orderId();

        return repository.findByOrderId(orderId)
                .orElseGet(() -> createAnalysis(event));
    }

    public List<FraudAnalysis> listAll() {
        return repository.listAll();
    }

    public FraudAnalysis findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException(
                        "Análise de fraude não encontrada para o pedido " + orderId
                ));
    }

    private FraudAnalysis createAnalysis(OrderCreatedEvent event) {
        FraudDecision decision = analyzeWithModelOrRules(event);

        FraudAnalysis analysis = new FraudAnalysis();

        analysis.eventId = event.eventId();
        analysis.orderId = event.payload().orderId();
        analysis.userId = event.payload().userId();
        analysis.customerState = event.payload().customerState();
        analysis.totalAmount = event.payload().totalAmount();
        analysis.riskScore = decision.riskScore();
        analysis.status = decision.status();
        analysis.reason = decision.reason();

        repository.persist(analysis);
        metricsService.recordPrediction(
                decision.status(),
                decision.riskScore(),
                decision.source(),
                decision.modelVersion()
        );

        return analysis;
    }

    private FraudDecision analyzeWithModelOrRules(OrderCreatedEvent event) {
        if (fraudTribuoModelService.isReady()) {
            FraudPredictionResult prediction = fraudTribuoModelService.predict(
                    event.payload().totalAmount(),
                    calculateItemsQuantity(event),
                    calculateAvgItemPrice(event),
                    calculateMaxItemPrice(event),
                    calculateUniqueProducts(event),
                    findOriginState(event),
                    event.payload().customerState()
            ).orElse(null);

            if (prediction != null) {
                BigDecimal riskScore = BigDecimal
                        .valueOf(prediction.riskScore() * 100)
                        .setScale(2, RoundingMode.HALF_UP);

                FraudStatus status = prediction.fraudRisk()
                        ? FraudStatus.REJECTED
                        : FraudStatus.APPROVED;

                String reason = prediction.reason()
                        + " Label: " + prediction.label()
                        + ". Modelo: " + prediction.modelVersion()
                        + ". Score de risco: " + riskScore
                        + ". Valor total: " + event.payload().totalAmount();

                return new FraudDecision(
                        status,
                        riskScore,
                        reason,
                        "TRIBUO_MODEL",
                        prediction.modelVersion()
                );
            }
        }

        BigDecimal riskScore = calculateRiskScore(event);

        FraudStatus status = riskScore.compareTo(RULE_REJECTION_THRESHOLD) >= 0
                ? FraudStatus.REJECTED
                : FraudStatus.APPROVED;

        return new FraudDecision(
                status,
                riskScore,
                buildReason(status, riskScore, event.payload().totalAmount()),
                "RULE_FALLBACK",
                "rules-v1"
        );
    }

    private BigDecimal calculateRiskScore(OrderCreatedEvent event) {
        BigDecimal totalAmount = event.payload().totalAmount();

        BigDecimal score = BigDecimal.ZERO;

        if (totalAmount.compareTo(new BigDecimal("1000.00")) > 0) {
            score = score.add(new BigDecimal("35.00"));
        }

        if (totalAmount.compareTo(new BigDecimal("3000.00")) > 0) {
            score = score.add(new BigDecimal("35.00"));
        }

        Integer totalItems = calculateItemsQuantity(event);

        if (totalItems > 5) {
            score = score.add(new BigDecimal("20.00"));
        }

        if (event.payload().estimatedDeliveryDays() != null
                && event.payload().estimatedDeliveryDays() > 15) {
            score = score.add(new BigDecimal("10.00"));
        }

        if (score.compareTo(new BigDecimal("100.00")) > 0) {
            score = new BigDecimal("100.00");
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private Integer calculateItemsQuantity(OrderCreatedEvent event) {
        if (event.payload().items() == null || event.payload().items().isEmpty()) {
            return 1;
        }

        int total = event.payload().items()
                .stream()
                .mapToInt(item -> item.quantity() == null ? 0 : item.quantity())
                .sum();

        return Math.max(1, total);
    }

    private BigDecimal calculateAvgItemPrice(OrderCreatedEvent event) {
        BigDecimal totalAmount = event.payload().totalAmount() == null
                ? BigDecimal.ZERO
                : event.payload().totalAmount();

        int itemsQuantity = Math.max(1, calculateItemsQuantity(event));

        return totalAmount.divide(
                BigDecimal.valueOf(itemsQuantity),
                2,
                RoundingMode.HALF_UP
        );
    }

    private BigDecimal calculateMaxItemPrice(OrderCreatedEvent event) {
        if (event.payload().items() == null || event.payload().items().isEmpty()) {
            return event.payload().totalAmount() == null
                    ? BigDecimal.ZERO
                    : event.payload().totalAmount();
        }

        return event.payload().items()
                .stream()
                .map(item -> item.unitPrice() == null ? BigDecimal.ZERO : item.unitPrice())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private Integer calculateUniqueProducts(OrderCreatedEvent event) {
        if (event.payload().items() == null || event.payload().items().isEmpty()) {
            return 1;
        }

        return (int) event.payload().items()
                .stream()
                .map(item -> item.productId())
                .distinct()
                .count();
    }

    private String findOriginState(OrderCreatedEvent event) {
        if (event.payload().items() == null || event.payload().items().isEmpty()) {
            return "SP";
        }

        return event.payload().items()
                .stream()
                .map(item -> item.originState())
                .filter(origin -> origin != null && !origin.isBlank())
                .findFirst()
                .orElse("SP");
    }

    private String buildReason(FraudStatus status, BigDecimal riskScore, BigDecimal totalAmount) {
        if (FraudStatus.REJECTED.equals(status)) {
            return "Pedido rejeitado por score de risco elevado: "
                    + riskScore
                    + ". Valor total: "
                    + totalAmount;
        }

        return "Pedido aprovado na análise de fraude. Score de risco: "
                + riskScore
                + ". Valor total: "
                + totalAmount;
    }

    private void validateEvent(OrderCreatedEvent event) {
        if (event == null || event.payload() == null) {
            throw new BadRequestException("Evento order.created inválido");
        }

        if (event.payload().orderId() == null) {
            throw new BadRequestException("Evento order.created sem orderId");
        }

        if (event.payload().userId() == null) {
            throw new BadRequestException("Evento order.created sem userId");
        }

        if (event.payload().totalAmount() == null) {
            throw new BadRequestException("Evento order.created sem totalAmount");
        }
    }

    private record FraudDecision(
            FraudStatus status,
            BigDecimal riskScore,
            String reason,
            String source,
            String modelVersion
    ) {
    }
}