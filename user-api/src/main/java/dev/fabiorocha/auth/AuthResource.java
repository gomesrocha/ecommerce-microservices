package dev.fabiorocha.auth;


import dev.fabiorocha.common.ErrorResponse;
import dev.fabiorocha.user.entity.UserEntity;
import io.smallrye.jwt.auth.principal.JWTParser;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/auth")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    @Inject
    AuthService authService;

    @Inject
    JWTParser jwtParser;

    @POST
    @Path("/login")
    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserEntity user = authService.autenticar(request.username(), request.password())
                .orElseThrow(() -> erro401("Usuário ou senha inválidos"));

        return authService.gerarTokens(user, true);
    }

    @POST
    @Path("/refresh-token")
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        try {
            JsonWebToken token = jwtParser.parse(request.refreshToken());

            String tipo = token.getClaim("tipo");

            if (!"refresh_token".equals(tipo)) {
                throw erro401("Token inválido para refresh");
            }

            UserEntity user = UserEntity.buscarPorUsername(token.getSubject())
                    .orElseThrow(() -> erro401("Usuário não encontrado"));

            return authService.gerarTokens(user, false);

        } catch (Exception e) {
            throw erro401("Refresh token inválido");
        }
    }

    private WebApplicationException erro401(String mensagem) {
        return new WebApplicationException(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity(new ErrorResponse(mensagem))
                        .build()
        );
    }
}