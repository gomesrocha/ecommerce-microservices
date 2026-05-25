package br.com.ecommerce.error;

import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static final Logger LOG = Logger.getLogger(PersistenceExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(PersistenceException exception) {
        LOG.error("Erro de persistência", exception);

        return ErrorResponseFactory.build(
                Response.Status.CONFLICT,
                "PERSISTENCE_ERROR",
                "Não foi possível persistir os dados. Verifique conflitos ou restrições no banco.",
                uriInfo
        );
    }
}