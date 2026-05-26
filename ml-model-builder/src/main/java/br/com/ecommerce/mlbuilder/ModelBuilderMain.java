package br.com.ecommerce.mlbuilder;

import br.com.ecommerce.mlbuilder.delivery.DeliveryModelTrainer;
import br.com.ecommerce.mlbuilder.fraud.FraudModelTrainer;

import java.nio.file.Path;
import java.util.Arrays;

public class ModelBuilderMain {

    private static final Path REPO_ROOT = resolveRepoRoot();

    private static final Path DELIVERY_INPUT = REPO_ROOT.resolve(
            "delivery-estimator-api/src/main/resources/ml/olist_delivery_training_tribuo.csv"
    );

    private static final Path DELIVERY_OUTPUT = REPO_ROOT.resolve(
            "models/delivery/delivery-tribuo-v1.model"
    );

    private static final Path FRAUD_INPUT = REPO_ROOT.resolve(
            "data/fraud/olist/processed/olist_fraud_risk_training_tribuo.csv"
    );

    private static final Path FRAUD_OUTPUT = REPO_ROOT.resolve(
            "models/fraud/fraud-tribuo-v1.model"
    );

    public static void main(String[] args) {
        String command = args.length == 0 ? "train-all" : args[0];

        System.out.println("ML Model Builder");
        System.out.println("Comando: " + command);
        System.out.println("Args: " + Arrays.toString(args));
        System.out.println("Repo root: " + REPO_ROOT);
        System.out.println();

        switch (command) {
            case "train-delivery" -> trainDelivery();
            case "train-fraud" -> trainFraud();
            case "train-all" -> {
                trainDelivery();
                trainFraud();
            }
            default -> {
                printUsage();
                throw new IllegalArgumentException("Comando inválido: " + command);
            }
        }
    }

    private static void trainDelivery() {
        new DeliveryModelTrainer().train(DELIVERY_INPUT, DELIVERY_OUTPUT);
    }

    private static void trainFraud() {
        new FraudModelTrainer().train(FRAUD_INPUT, FRAUD_OUTPUT);
    }

    private static Path resolveRepoRoot() {
        Path currentDir = Path.of("").toAbsolutePath().normalize();

        if ("ml-model-builder".equals(currentDir.getFileName().toString())) {
            return currentDir.getParent();
        }

        return currentDir;
    }

    private static void printUsage() {
        System.out.println("Uso:");
        System.out.println("  mvn exec:java -Dexec.args=\"train-delivery\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-fraud\"");
        System.out.println("  mvn exec:java -Dexec.args=\"train-all\"");
        System.out.println();
        System.out.println("Também pode executar da raiz:");
        System.out.println("  mvn -f ml-model-builder/pom.xml exec:java -Dexec.args=\"train-all\"");
    }
}