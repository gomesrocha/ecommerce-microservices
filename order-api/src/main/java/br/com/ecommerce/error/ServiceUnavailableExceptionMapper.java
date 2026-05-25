package br.com.ecommerce.error;

import jakarta.ws.rs.ServiceUnavailableException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServiceUnavailableExceptionMapper implements ExceptionMapper<ServiceUnavailableException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ServiceUnavailableException exception) {
        return ErrorResponseFactory.build(
                Response.Status.SERVICE_UNAVAILABLE,
                "SERVICE_UNAVAILABLE",
                getMessage(exception, "Serviço temporariamente indisponível"),
                uriInfo
        );
    }

    private String getMessage(Throwable exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}