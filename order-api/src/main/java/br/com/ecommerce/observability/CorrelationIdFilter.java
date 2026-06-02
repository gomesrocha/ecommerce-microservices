package br.com.ecommerce.observability;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.io.IOException;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(CorrelationIdFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String correlationId = requestContext.getHeaderString(CorrelationIdContext.HEADER_NAME);

        CorrelationIdContext.set(correlationId);

        LOG.debugf(
                "CorrelationId recebido/gerado. method=%s, path=%s, correlationId=%s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                CorrelationIdContext.getOrCreate()
        );
    }

    @Override
    public void filter(
            ContainerRequestContext requestContext,
            ContainerResponseContext responseContext
    ) throws IOException {
        String correlationId = CorrelationIdContext.getOrCreate();

        responseContext.getHeaders().putSingle(
                CorrelationIdContext.HEADER_NAME,
                correlationId
        );

        CorrelationIdContext.clear();
    }
}