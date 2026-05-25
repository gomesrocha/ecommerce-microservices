package br.com.ecommerce.error;

import io.quarkus.hibernate.validator.runtime.jaxrs.ResteasyReactiveViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.List;

@Provider
public class ResteasyReactiveViolationExceptionMapper implements ExceptionMapper<ResteasyReactiveViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ResteasyReactiveViolationException exception) {
        List<String> details = exception
                .getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return ErrorResponseFactory.build(
                Response.Status.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Dados inválidos na requisição",
                uriInfo,
                details
        );
    }
}