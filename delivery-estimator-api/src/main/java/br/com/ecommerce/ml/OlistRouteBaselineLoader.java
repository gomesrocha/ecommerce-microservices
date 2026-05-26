package br.com.ecommerce.ml;

import br.com.ecommerce.dto.UpsertDeliveryRouteRequest;
import br.com.ecommerce.service.DeliveryEstimateService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class OlistRouteBaselineLoader {

    private static final Logger LOG = Logger.getLogger(OlistRouteBaselineLoader.class);

    @Inject
    DeliveryEstimateService deliveryEstimateService;

    @ConfigProperty(name = "app.olist-baseline.min-samples", defaultValue = "5")
    Long minSamples;

    @Transactional
    public int loadFromClasspath(String resourcePath) {
        InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream == null) {
            LOG.warnf("Arquivo de baseline Olist não encontrado no classpath: %s", resourcePath);
            return 0;
        }

        int imported = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                if (header) {
                    header = false;
                    continue;
                }

                OlistRouteBaselineRow row = OlistRouteBaselineRow.fromCsvLine(line);

                if (row.samples() < minSamples) {
                    continue;
                }

                UpsertDeliveryRouteRequest request = new UpsertDeliveryRouteRequest(
                        row.originState(),
                        row.destinationState(),
                        row.minDays(),
                        row.estimatedDays(),
                        row.maxDays(),
                        row.source(),
                        row.modelVersion()
                );

                deliveryEstimateService.upsertRoute(request);
                imported++;
            }

            LOG.infof("Baseline Olist carregado com sucesso. Rotas importadas: %d", imported);
            return imported;

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao carregar baseline Olist", exception);
        }
    }
}