package br.com.ecommerce.service.resilience;

import br.com.ecommerce.client.ProductClient;
import br.com.ecommerce.client.ProductClientResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ProductCatalogGateway {

    private static final Logger LOG = Logger.getLogger(ProductCatalogGateway.class);

    @Inject
    @RestClient
    ProductClient productClient;

    @Timeout(1000)
    @Retry(
            maxRetries = 2,
            delay = 200,
            retryOn = {ProcessingException.class},
            abortOn = {WebApplicationException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 4,
            delay = 5000,
            skipOn = {WebApplicationException.class}
    )
    @Fallback(
            fallbackMethod = "fallbackProduct",
            skipOn = {WebApplicationException.class}
    )
    public ProductClientResponse getProductById(Long productId) {
        return productClient.findById(productId);
    }

    public ProductClientResponse fallbackProduct(Long productId) {
        LOG.errorf(
                "Fallback acionado para consulta de produto. productId=%s",
                productId
        );

        throw new ServiceUnavailableException(
                "product-api indisponível no momento. Não foi possível validar o produto " + productId
        );
    }
}