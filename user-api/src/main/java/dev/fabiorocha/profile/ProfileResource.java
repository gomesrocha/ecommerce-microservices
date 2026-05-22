package dev.fabiorocha.profile;


import dev.fabiorocha.common.ErrorResponse;
import dev.fabiorocha.user.entity.UserEntity;
import dev.fabiorocha.user.dto.UserResponse;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/profile")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProfileResource {

    @Inject
    JsonWebToken jwt;

    @GET
    public UserResponse meuPerfil() {
        UserEntity user = usuarioLogado();
        return UserResponse.from(user);
    }

    @PUT
    @Transactional
    public UserResponse atualizarPerfil(ProfileUpdateRequest request) {
        UserEntity user = usuarioLogado();

        user.nome = request.nome();
        user.endereco = request.endereco();
        user.telefone = request.telefone();
        user.avatar = request.avatar();
        user.bio = request.bio();

        return UserResponse.from(user);
    }

    @PUT
    @Path("/password")
    @Transactional
    public Response alterarSenha(PasswordUpdateRequest request) {
        UserEntity user = usuarioLogado();

        if (!BcryptUtil.matches(request.senhaAtual(), user.passwordHash)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Senha atual inválida"))
                    .build();
        }

        if (!request.novaSenha().equals(request.confirmarNovaSenha())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("A confirmação da nova senha não confere"))
                    .build();
        }

        user.passwordHash = BcryptUtil.bcryptHash(request.novaSenha());

        return Response.ok(new ErrorResponse("Senha alterada com sucesso")).build();
    }

    private UserEntity usuarioLogado() {
        return UserEntity.buscarPorUsername(jwt.getSubject())
                .orElseThrow(() -> new NotFoundException(
                        Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse("Usuário logado não encontrado"))
                                .build()
                ));
    }
}