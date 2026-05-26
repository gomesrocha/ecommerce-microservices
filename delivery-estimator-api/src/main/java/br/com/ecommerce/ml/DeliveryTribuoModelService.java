package br.com.ecommerce.ml;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.tribuo.Model;
import org.tribuo.Prediction;
import org.tribuo.regression.Regressor;

import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@ApplicationScoped
public class DeliveryTribuoModelService {

    private static final Logger LOG = Logger.getLogger(DeliveryTribuoModelService.class);

    private static final String MODEL_VERSION = "delivery-tribuo-v1";

    private Model<Regressor> model;

    @ConfigProperty(name = "app.delivery-ml.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "app.delivery-ml.model-path")
    String modelPath;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.info("Modelo Tribuo de entrega desabilitado.");
            return;
        }

        try {
            loadModel();
        } catch (Exception exception) {
            LOG.errorf(
                    exception,
                    "Falha ao carregar modelo Tribuo de entrega em %s. O serviço continuará usando baseline/fallback.",
                    modelPath
            );
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

    @SuppressWarnings("unchecked")
    private void loadModel() throws Exception {
        Path path = Path.of(modelPath);

        if (!Files.exists(path)) {
            LOG.warnf("Modelo Tribuo de entrega não encontrado: %s", path.toAbsolutePath());
            return;
        }

        LOG.infof("Carregando modelo Tribuo de entrega: %s", path.toAbsolutePath());

        try (ObjectInputStream inputStream = new ObjectInputStream(Files.newInputStream(path))) {
            Object object = inputStream.readObject();

            if (!(object instanceof Model<?> loadedModel)) {
                throw new IllegalStateException("Arquivo não contém um modelo Tribuo válido: " + path);
            }

            this.model = (Model<Regressor>) loadedModel;
        }

        LOG.infof("Modelo Tribuo de entrega carregado com sucesso. modelVersion=%s", MODEL_VERSION);
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