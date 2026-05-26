package br.com.ecommerce.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int statusCode = exception.getResponse() != null
                ? exception.getResponse().getStatus()
                : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        Response.Status status = Response.Status.fromStatusCode(statusCode);

        if (status == null) {
            status = Response.Status.INTERNAL_SERVER_ERROR;
        }

        return ErrorResponseFactory.build(
                status,
                status.name(),
                getMessage(exception, status.getReasonPhrase()),
                uriInfo
        );
    }

    private String getMessage(Throwable exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }
}