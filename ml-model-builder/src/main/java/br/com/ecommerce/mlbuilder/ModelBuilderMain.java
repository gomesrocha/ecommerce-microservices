package br.com.ecommerce.mlbuilder;

import br.com.ecommerce.mlbuilder.delivery.DeliveryModelTrainer;
import br.com.ecommerce.mlbuilder.fraud.FraudModelTrainer;
import br.com.ecommerce.mlbuilder.report.TrainingReport;
import br.com.ecommerce.mlbuilder.report.TrainingReportWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ModelBuilderMain {

    private static final Path DELIVERY_INPUT = repoPath(
            "delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv"
    );

    private static final Path DELIVERY_OUTPUT = repoPath(
            "models/delivery/delivery-tribuo-v1.model"
    );

    private static final Path FRAUD_INPUT = repoPath(
            "data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv"
    );

    private static final Path FRAUD_OUTPUT = repoPath(
            "models/fraud/fraud-tribuo-v1.model"
    );

    private static final Path REPORTS_DIR = repoPath("reports/ml");

    public static void main(String[] args) {
        String command = args.length == 0 ? "train-all" : args[0];

        System.out.println("ML Model Builder");
        System.out.println("Comando: " + command);
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println();

        List<TrainingReport> reports = new ArrayList<>();

        switch (command) {
            case "train-delivery" -> reports.add(trainDelivery());
            case "train-fraud" -> reports.add(trainFraud());
            case "train-all" -> {
                reports.add(trainDelivery());
                reports.add(trainFraud());
            }
            default -> {
                printUsage();
                throw new IllegalArgumentException("Comando inválido: " + command);
            }
        }

        new TrainingReportWriter().writeAll(reports, REPORTS_DIR);
    }

    private static TrainingReport trainDelivery() {
        return new DeliveryModelTrainer().train(DELIVERY_INPUT, DELIVERY_OUTPUT);
    }

    private static TrainingReport trainFraud() {
        new FraudModelTrainer().train(FRAUD_INPUT, FRAUD_OUTPUT);

        long totalRecords = countCsvRecords(FRAUD_INPUT);

        return new TrainingReport(
                "fraud-tribuo",
                "fraud-tribuo-v1",
                "FRAUD",
                "Tribuo Classification",
                "Tribuo",
                FRAUD_INPUT.getFileName().toString(),
                FRAUD_INPUT.toString(),
                FRAUD_OUTPUT.toString(),
                "ACTIVE",
                Instant.now(),
                0,
                0,
                totalRecords,
                List.of(
                        "totalAmount",
                        "totalItems",
                        "customerState",
                        "estimatedDeliveryDays"
                ),
                "riskLabel",
                Map.of(
                        "accuracy", "not_available",
                        "precision", "not_available",
                        "recall", "not_available",
                        "f1", "not_available"
                ),
                "Modelo de classificação de risco de fraude usando dataset sintético baseado em atributos do ecommerce e da base Olist. Métricas detalhadas serão extraídas em uma evolução do FraudModelTrainer."
        );
    }

    private static Path repoPath(String path) {
        return Path.of("..").resolve(path).normalize();
    }

    private static long countCsvRecords(Path input) {
        try (var lines = Files.lines(input)) {
            return Math.max(0, lines.count() - 1);
        } catch (Exception exception) {
            return 0;
        }
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  mvn exec:java -Dexec.args=\"train-delivery\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-fraud\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-all\"");
    }
}