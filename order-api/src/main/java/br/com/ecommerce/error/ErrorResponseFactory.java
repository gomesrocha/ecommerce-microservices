package br.com.ecommerce.error;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static Response build(
            Response.Status status,
            String error,
            String message,
            UriInfo uriInfo
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                status.getStatusCode(),
                error,
                message,
                getPath(uriInfo)
        );

        return Response
                .status(status)
                .entity(response)
                .build();
    }

    public static Response build(
            Response.Status status,
            String error,
            String message,
            UriInfo uriInfo,
            List<String> details
    ) {
        ApiErrorResponse response = ApiErrorResponse.of(
                status.getStatusCode(),
                error,
                message,
                getPath(uriInfo),
                details
        );

        return Response
                .status(status)
                .entity(response)
                .build();
    }

    private static String getPath(UriInfo uriInfo) {
        if (uriInfo == null || uriInfo.getRequestUri() == null) {
            return "";
        }

        return uriInfo.getRequestUri().getPath();
    }
}