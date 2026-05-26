package br.com.ecommerce.mlbuilder.delivery;

import br.com.ecommerce.mlbuilder.common.ModelIO;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.evaluation.RegressionEvaluator;
import org.tribuo.regression.rtree.CARTRegressionTrainer;

import java.nio.file.Files;
import java.nio.file.Path;

public class DeliveryModelTrainer {

    private static final String TARGET_COLUMN = "delivery_days";

    public void train(Path inputCsv, Path outputModel) {
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

            System.out.printf(
                    "Delivery model metrics: trainSize=%d, testSize=%d, averageRMSE=%.3f, averageMAE=%.3f, averageR2=%.3f%n",
                    trainData.size(),
                    testData.size(),
                    evaluation.averageRMSE(),
                    evaluation.averageMAE(),
                    evaluation.averageR2()
            );

            ModelIO.saveModel(model, outputModel);

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao treinar modelo de delivery", exception);
        }
    }
}

