package br.com.ecommerce.mlbuilder;

import br.com.ecommerce.mlbuilder.delivery.DeliveryModelTrainer;
import br.com.ecommerce.mlbuilder.fraud.FraudModelTrainer;
import br.com.ecommerce.mlbuilder.report.TrainingReport;
import br.com.ecommerce.mlbuilder.report.TrainingReportWriter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelBuilderMain {

    private static final Path DELIVERY_INPUT = repoPath(
            "delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv"
    );

    private static final Path DELIVERY_OUTPUT = repoPath(
            "models/delivery/delivery-tribuo-v1.model"
    );

    private static final Path FRAUD_OLIST_INPUT = repoPath(
            "data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv"
    );

    private static final Path FRAUD_OLIST_OUTPUT = repoPath(
            "models/fraud/fraud-tribuo-v1.model"
    );

    private static final Path FRAUD_KAGGLE_INPUT = repoPath(
            "data/fraud/kaggle/processed/kaggle_fraud_training_tribuo.csv"
    );

    private static final Path FRAUD_KAGGLE_OUTPUT = repoPath(
            "models/fraud/fraud-tribuo-v2.model"
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

            case "train-fraud" -> reports.add(trainFraudOlist());

            case "train-fraud-olist" -> reports.add(trainFraudOlist());

            case "train-fraud-kaggle" -> reports.add(trainFraudKaggle());

            case "train-all" -> {
                reports.add(trainDelivery());
                reports.add(trainFraudOlist());
            }

            case "train-all-kaggle" -> {
                reports.add(trainDelivery());
                reports.add(trainFraudKaggle());
            }

            case "train-all-models" -> {
                reports.add(trainDelivery());
                reports.add(trainFraudOlist());
                reports.add(trainFraudKaggle());
            }

            default -> {
                printUsage();
                throw new IllegalArgumentException("Comando inválido: " + command);
            }
        }

        new TrainingReportWriter().writeAll(reports, REPORTS_DIR);
    }

    private static TrainingReport trainDelivery() {
        return new DeliveryModelTrainer().train(
                DELIVERY_INPUT,
                DELIVERY_OUTPUT
        );
    }

    private static TrainingReport trainFraudOlist() {
        return new FraudModelTrainer().train(
                FRAUD_OLIST_INPUT,
                FRAUD_OLIST_OUTPUT
        );
    }

    private static TrainingReport trainFraudKaggle() {
        return new FraudModelTrainer().train(
                FRAUD_KAGGLE_INPUT,
                FRAUD_KAGGLE_OUTPUT,
                "fraud-tribuo",
                "fraud-tribuo-v2",
                "Modelo de classificação de risco de fraude treinado com dataset público Kaggle Credit Card Transactions Fraud Detection, adaptado para um schema canônico compatível com OrderCreatedEvent. As métricas usam FRAUD_RISK como classe positiva."
        );
    }

    private static Path repoPath(String path) {
        return Path.of("..").resolve(path).normalize();
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  mvn exec:java -Dexec.args=\"train-delivery\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-fraud\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-fraud-olist\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-fraud-kaggle\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-all\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-all-kaggle\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-all-models\"");
    }
}