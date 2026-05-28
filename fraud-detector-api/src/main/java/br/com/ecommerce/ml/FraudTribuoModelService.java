package br.com.ecommerce.ml;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;

import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@ApplicationScoped
public class FraudTribuoModelService {

    private static final Logger LOG = Logger.getLogger(FraudTribuoModelService.class);

    private static final String MODEL_VERSION = "fraud-tribuo-v1";
    private static final String FRAUD_RISK = "FRAUD_RISK";

    private Model<Label> model;

    @ConfigProperty(name = "app.fraud-ml.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "app.fraud-ml.model-path")
    String modelPath;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.info("Modelo Tribuo de fraude desabilitado.");
            return;
        }

        try {
            loadModel();
        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao carregar modelo Tribuo de fraude em %s. O serviço continuará usando regra/fallback.",
                    modelPath
            );
        }
    }

    public boolean isReady() {
        return model != null;
    }

    public Optional<FraudPredictionResult> predict(
            BigDecimal totalAmount,
            Integer itemsQuantity,
            BigDecimal avgItemPrice,
            BigDecimal maxItemPrice,
            Integer uniqueProducts,
            String originState,
            String destinationState
    ) {
        if (model == null) {
            return Optional.empty();
        }

        var example = FraudMlFeatureBuilder.buildExample(
                totalAmount,
                itemsQuantity,
                avgItemPrice,
                maxItemPrice,
                uniqueProducts,
                originState,
                destinationState
        );

        Prediction<Label> prediction = model.predict(example);

        Label output = prediction.getOutput();

        String label = output.getLabel();
        double confidence = sanitizeScore(output.getScore());

        boolean fraudRisk = FRAUD_RISK.equals(label);

        double riskScore = fraudRisk
                ? Math.max(0.75, confidence)
                : Math.min(0.25, 1.0 - confidence);

        String reason = fraudRisk
                ? "Pedido classificado como risco de fraude pelo modelo Tribuo."
                : "Pedido classificado como legítimo pelo modelo Tribuo.";

        return Optional.of(new FraudPredictionResult(
                label,
                fraudRisk,
                riskScore,
                reason,
                MODEL_VERSION
        ));
    }

    @SuppressWarnings("unchecked")
    private void loadModel() throws Exception {
        Path path = Path.of(modelPath);

        if (!Files.exists(path)) {
            LOG.warnf("Modelo Tribuo de fraude não encontrado: %s", path.toAbsolutePath());
            return;
        }

        LOG.infof("Carregando modelo Tribuo de fraude: %s", path.toAbsolutePath());

        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(path))) {
            Object object = inputStream.readObject();

            if (!(object instanceof Model<?> loadedModel)) {
                throw new IllegalStateException("Arquivo não contém um modelo Tribuo válido: " + path);
            }

            this.model = (Model<Label>) loadedModel;
        }

        LOG.infof("Modelo Tribuo de fraude carregado com sucesso. modelVersion=%s", MODEL_VERSION);
    }

    private double sanitizeScore(double value) {
        if (!Double.isFinite(value)) {
            return 0.5;
        }

        return Math.min(1.0, Math.max(0.0, value));
    }
}