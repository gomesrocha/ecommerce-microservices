package br.com.ecommerce.ml;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.tribuo.Dataset;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.evaluation.RegressionEvaluator;
import org.tribuo.regression.rtree.CARTRegressionTrainer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

@ApplicationScoped
public class DeliveryTribuoModelService {

    private static final Logger LOG = Logger.getLogger(DeliveryTribuoModelService.class);

    private static final String TARGET_COLUMN = "delivery_days";
    private static final String MODEL_VERSION = "delivery-tribuo-v1";

    private Model<Regressor> model;

    @ConfigProperty(name = "app.delivery-ml.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(
            name = "app.delivery-ml.training-resource",
            defaultValue = "ml/olist_delivery_training_tribuo.csv"
    )
    String trainingResource;

    @ConfigProperty(name = "app.delivery-ml.tree-depth", defaultValue = "8")
    int treeDepth;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.info("Modelo Tribuo de entrega desabilitado.");
            return;
        }

        try {
            trainModel();
        } catch (Exception exception) {
            LOG.error("Falha ao treinar modelo Tribuo de entrega. O serviço continuará usando baseline/fallback.", exception);
        }
    }

    public boolean isReady() {
        return model != null;
    }

    public Optional<DeliveryPredictionResult> predict(
            String originState,
            String destinationState,
            Integer totalItems
    ) {
        if (model == null) {
            return Optional.empty();
        }

        var example = DeliveryMlFeatureBuilder.buildExample(
                originState,
                destinationState,
                totalItems
        );

        Prediction<Regressor> prediction = model.predict(example);

        double value = prediction.getOutput()
                .getValues()[0];

        int estimated = sanitizeEstimatedDays(value);

        return Optional.of(new DeliveryPredictionResult(
                Math.max(1, estimated - 2),
                estimated,
                Math.max(estimated + 1, estimated + 4),
                "TRIBUO_MODEL",
                MODEL_VERSION
        ));
    }

    private void trainModel() throws Exception {
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(trainingResource);

        if (inputStream == null) {
            LOG.warnf("CSV de treino Tribuo não encontrado: %s", trainingResource);
            return;
        }

        Path csvPath = Files.createTempFile("olist-delivery-training-", ".csv");

        try (inputStream) {
            Files.copy(inputStream, csvPath, StandardCopyOption.REPLACE_EXISTING);
        }

        LOG.infof("Treinando modelo Tribuo de entrega. csv=%s, treeDepth=%d", csvPath, treeDepth);

        RegressionFactory factory = new RegressionFactory();
        CSVLoader<Regressor> loader = new CSVLoader<>(factory);

        var dataSource = loader.loadDataSource(csvPath, TARGET_COLUMN);

        TrainTestSplitter<Regressor> splitter = new TrainTestSplitter<>(
                dataSource,
                0.8,
                42L
        );

        Dataset<Regressor> trainData = new MutableDataset<>(splitter.getTrain());
        Dataset<Regressor> testData = new MutableDataset<>(splitter.getTest());

        CARTRegressionTrainer trainer = new CARTRegressionTrainer(treeDepth);

        Model<Regressor> trainedModel = trainer.train(trainData);

        RegressionEvaluator evaluator = new RegressionEvaluator();
        var evaluation = evaluator.evaluate(trainedModel, testData);

        LOG.infof(
                "Modelo Tribuo treinado. trainSize=%d, testSize=%d, averageRMSE=%.3f, averageMAE=%.3f, averageR2=%.3f",
                trainData.size(),
                testData.size(),
                evaluation.averageRMSE(),
                evaluation.averageMAE(),
                evaluation.averageR2()
        );

        this.model = trainedModel;
    }

    private int sanitizeEstimatedDays(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 10;
        }

        int rounded = (int) Math.round(value);

        if (rounded < 1) {
            return 1;
        }

        if (rounded > 90) {
            return 90;
        }

        return rounded;
    }
}