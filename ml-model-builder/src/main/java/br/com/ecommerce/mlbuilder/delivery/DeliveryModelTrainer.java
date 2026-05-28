package br.com.ecommerce.mlbuilder.delivery;

import br.com.ecommerce.mlbuilder.common.ModelIO;
import br.com.ecommerce.mlbuilder.report.TrainingReport;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.evaluation.RegressionEvaluator;
import org.tribuo.regression.rtree.CARTRegressionTrainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeliveryModelTrainer {

    private static final String TARGET_COLUMN = "delivery_days";

    public TrainingReport train(Path inputCsv, Path outputModel) {
        try {
            if (!Files.exists(inputCsv)) {
                throw new IllegalArgumentException("CSV de treino de delivery não encontrado: " + inputCsv);
            }

            System.out.println("Treinando modelo de entrega...");
            System.out.println("Input CSV: " + inputCsv.toAbsolutePath());

            RegressionFactory factory = new RegressionFactory();
            CSVLoader<Regressor> loader = new CSVLoader<>(factory);

            var dataSource = loader.loadDataSource(inputCsv, TARGET_COLUMN);

            TrainTestSplitter<Regressor> splitter = new TrainTestSplitter<>(
                    dataSource,
                    0.8,
                    42L
            );

            Dataset<Regressor> trainData = new MutableDataset<>(splitter.getTrain());
            Dataset<Regressor> testData = new MutableDataset<>(splitter.getTest());

            CARTRegressionTrainer trainer = new CARTRegressionTrainer(8);

            Model<Regressor> model = trainer.train(trainData);

            RegressionEvaluator evaluator = new RegressionEvaluator();
            var evaluation = evaluator.evaluate(model, testData);

            double averageRMSE = evaluation.averageRMSE();
            double averageMAE = evaluation.averageMAE();
            double averageR2 = evaluation.averageR2();

            long trainSize = trainData.size();
            long testSize = testData.size();

            System.out.printf(
                    "Delivery model metrics: trainSize=%d, testSize=%d, averageRMSE=%.3f, averageMAE=%.3f, averageR2=%.3f%n",
                    trainSize,
                    testSize,
                    averageRMSE,
                    averageMAE,
                    averageR2
            );

            ModelIO.saveModel(model, outputModel);

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("rmse", averageRMSE);
            metrics.put("mae", averageMAE);
            metrics.put("r2", averageR2);

            return new TrainingReport(
                    "delivery-tribuo",
                    "delivery-tribuo-v1",
                    "DELIVERY",
                    "Tribuo CART Regression",
                    "Tribuo",
                    inputCsv.getFileName().toString(),
                    inputCsv.toString(),
                    outputModel.toString(),
                    "ACTIVE",
                    Instant.now(),
                    trainSize,
                    testSize,
                    trainSize + testSize,
                    readFeatureNames(inputCsv),
                    TARGET_COLUMN,
                    metrics,
                    "Modelo de regressão para estimativa de prazo de entrega usando dados derivados da base Olist."
            );

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao treinar modelo de delivery", exception);
        }
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
}