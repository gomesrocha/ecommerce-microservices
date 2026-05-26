package br.com.ecommerce.mlbuilder.fraud;

import br.com.ecommerce.mlbuilder.common.ModelIO;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.dtree.CARTClassificationTrainer;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;

import java.nio.file.Files;
import java.nio.file.Path;

public class FraudModelTrainer {

    private static final String TARGET_COLUMN = "label";

    public void train(Path inputCsv, Path outputModel) {
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

            ModelIO.saveModel(model, outputModel);

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao treinar modelo de fraude", exception);
        }
    }
}
