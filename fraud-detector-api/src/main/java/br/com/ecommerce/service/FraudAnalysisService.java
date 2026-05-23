package br.com.ecommerce.service;

import br.com.ecommerce.domain.FraudAnalysis;
import br.com.ecommerce.domain.FraudStatus;
import br.com.ecommerce.event.OrderCreatedEvent;
import br.com.ecommerce.repository.FraudAnalysisRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@ApplicationScoped
public class FraudAnalysisService {

    @Inject
    FraudAnalysisRepository repository;

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
                .orElseThrow(() -> new BadRequestException("Análise de fraude não encontrada para o pedido " + orderId));
    }

    private FraudAnalysis createAnalysis(OrderCreatedEvent event) {
        BigDecimal riskScore = calculateRiskScore(event);
        FraudStatus status = riskScore.compareTo(new BigDecimal("70.00")) >= 0
                ? FraudStatus.REJECTED
                : FraudStatus.APPROVED;

        FraudAnalysis analysis = new FraudAnalysis();

        analysis.eventId = event.eventId();
        analysis.orderId = event.payload().orderId();
        analysis.userId = event.payload().userId();
        analysis.customerState = event.payload().customerState();
        analysis.totalAmount = event.payload().totalAmount();
        analysis.riskScore = riskScore;
        analysis.status = status;
        analysis.reason = buildReason(status, riskScore, analysis.totalAmount);

        repository.persist(analysis);

        return analysis;
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

        Integer totalItems = event.payload().items() == null
                ? 0
                : event.payload().items()
                    .stream()
                    .mapToInt(item -> item.quantity() == null ? 0 : item.quantity())
                    .sum();

        if (totalItems > 5) {
            score = score.add(new BigDecimal("20.00"));
        }

        if (event.payload().estimatedDeliveryDays() != null && event.payload().estimatedDeliveryDays() > 15) {
            score = score.add(new BigDecimal("10.00"));
        }

        if (score.compareTo(new BigDecimal("100.00")) > 0) {
            score = new BigDecimal("100.00");
        }

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildReason(FraudStatus status, BigDecimal riskScore, BigDecimal totalAmount) {
        if (FraudStatus.REJECTED.equals(status)) {
            return "Pedido rejeitado por score de risco elevado: " + riskScore + ". Valor total: " + totalAmount;
        }

        return "Pedido aprovado na análise de fraude. Score de risco: " + riskScore + ". Valor total: " + totalAmount;
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
}