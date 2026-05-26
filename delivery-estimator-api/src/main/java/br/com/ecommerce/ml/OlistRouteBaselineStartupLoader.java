package br.com.ecommerce.ml;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class OlistRouteBaselineStartupLoader {

    private static final Logger LOG = Logger.getLogger(OlistRouteBaselineStartupLoader.class);

    @Inject
    OlistRouteBaselineLoader loader;

    @ConfigProperty(name = "app.olist-baseline.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(
            name = "app.olist-baseline.resource",
            defaultValue = "ml/olist_delivery_route_baseline.csv"
    )
    String resource;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            LOG.info("Carga do baseline Olist desabilitada.");
            return;
        }

        LOG.infof("Iniciando carga do baseline Olist. resource=%s", resource);

        int imported = loader.loadFromClasspath(resource);

        LOG.infof("Carga do baseline Olist finalizada. Rotas importadas: %d", imported);
    }
}