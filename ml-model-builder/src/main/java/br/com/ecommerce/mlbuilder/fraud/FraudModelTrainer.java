package br.com.ecommerce.mlbuilder.fraud;

import br.com.ecommerce.mlbuilder.common.ModelIO;
import br.com.ecommerce.mlbuilder.report.TrainingReport;
import org.tribuo.Dataset;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.dtree.CARTClassificationTrainer;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FraudModelTrainer {

    private static final String TARGET_COLUMN = "label";
    private static final String POSITIVE_LABEL = "FRAUD_RISK";

    public TrainingReport train(Path inputCsv, Path outputModel) {
        try {
            if (!Files.exists(inputCsv)) {
                throw new IllegalArgumentException("CSV de treino de fraude não encontrado: " + inputCsv);
            }

            System.out.println("Treinando modelo de fraude...");
            System.out.println("Input CSV: " + inputCsv.toAbsolutePath());

            LabelFactory factory = new LabelFactory();
            CSVLoader<Label> loader = new CSVLoader<>(factory);

            var dataSource = loader.loadDataSource(inputCsv, TARGET_COLUMN);

            TrainTestSplitter<Label> splitter = new TrainTestSplitter<>(
                    dataSource,
                    0.8,
                    42L
            );

            Dataset<Label> trainData = new MutableDataset<>(splitter.getTrain());
            Dataset<Label> testData = new MutableDataset<>(splitter.getTest());

            CARTClassificationTrainer trainer = new CARTClassificationTrainer(8);

            Model<Label> model = trainer.train(trainData);

            LabelEvaluator evaluator = new LabelEvaluator();
            var evaluation = evaluator.evaluate(model, testData);

            System.out.println("Fraud model metrics:");
            System.out.println(evaluation.toString());

            ClassificationMetrics metrics = calculateMetrics(model, testData);

            System.out.printf(
                    "Fraud model summary: trainSize=%d, testSize=%d, accuracy=%.4f, precision=%.4f, recall=%.4f, f1=%.4f%n",
                    trainData.size(),
                    testData.size(),
                    metrics.accuracy(),
                    metrics.precision(),
                    metrics.recall(),
                    metrics.f1()
            );

            ModelIO.saveModel(model, outputModel);

            Map<String, Object> reportMetrics = new LinkedHashMap<>();
            reportMetrics.put("accuracy", metrics.accuracy());
            reportMetrics.put("precision", metrics.precision());
            reportMetrics.put("recall", metrics.recall());
            reportMetrics.put("f1", metrics.f1());
            reportMetrics.put("tp", metrics.truePositive());
            reportMetrics.put("fp", metrics.falsePositive());
            reportMetrics.put("fn", metrics.falseNegative());
            reportMetrics.put("tn", metrics.trueNegative());

            return new TrainingReport(
                    "fraud-tribuo",
                    "fraud-tribuo-v1",
                    "FRAUD",
                    "Tribuo CART Classification",
                    "Tribuo",
                    inputCsv.getFileName().toString(),
                    inputCsv.toString(),
                    outputModel.toString(),
                    "ACTIVE",
                    Instant.now(),
                    trainData.size(),
                    testData.size(),
                    trainData.size() + testData.size(),
                    readFeatureNames(inputCsv),
                    TARGET_COLUMN,
                    reportMetrics,
                    "Modelo de classificação de risco de fraude usando dataset sintético baseado em atributos do ecommerce e da base Olist. As métricas usam FRAUD_RISK como classe positiva."
            );

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao treinar modelo de fraude", exception);
        }
    }

    private ClassificationMetrics calculateMetrics(Model<Label> model, Dataset<Label> testData) {
        long truePositive = 0;
        long falsePositive = 0;
        long falseNegative = 0;
        long trueNegative = 0;

        for (Example<Label> example : testData) {
            Prediction<Label> prediction = model.predict(example);

            String actual = example.getOutput().getLabel();
            String predicted = prediction.getOutput().getLabel();

            boolean actualPositive = POSITIVE_LABEL.equals(actual);
            boolean predictedPositive = POSITIVE_LABEL.equals(predicted);

            if (actualPositive && predictedPositive) {
                truePositive++;
            } else if (!actualPositive && predictedPositive) {
                falsePositive++;
            } else if (actualPositive) {
                falseNegative++;
            } else {
                trueNegative++;
            }
        }

        long total = truePositive + falsePositive + falseNegative + trueNegative;
        long correct = truePositive + trueNegative;

        double accuracy = safeDivide(correct, total);
        double precision = safeDivide(truePositive, truePositive + falsePositive);
        double recall = safeDivide(truePositive, truePositive + falseNegative);
        double f1 = precision + recall == 0.0
                ? 0.0
                : 2.0 * precision * recall / (precision + recall);

        return new ClassificationMetrics(
                accuracy,
                precision,
                recall,
                f1,
                truePositive,
                falsePositive,
                falseNegative,
                trueNegative
        );
    }

    private double safeDivide(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }

        return (double) numerator / denominator;
    }

    private List<String> readFeatureNames(Path inputCsv) throws IOException {
        try (var lines = Files.lines(inputCsv)) {
            String header = lines.findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("CSV de treino vazio: " + inputCsv));

            return Arrays.stream(header.split(","))
                    .map(String::trim)
                    .filter(column -> !column.equals(TARGET_COLUMN))
                    .toList();
        }
    }

    private record ClassificationMetrics(
            double accuracy,
            double precision,
            double recall,
            double f1,
            long truePositive,
            long falsePositive,
            long falseNegative,
            long trueNegative
    ) {
    }
}
