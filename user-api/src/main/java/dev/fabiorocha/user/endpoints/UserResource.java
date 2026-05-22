package dev.fabiorocha.user.endpoints;

import dev.fabiorocha.user.dto.UserRequest;
import dev.fabiorocha.user.dto.UserResponse;
import dev.fabiorocha.user.entity.UserEntity;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

import dev.fabiorocha.common.ErrorResponse;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.transaction.Transactional;


@Path("/users")
@RolesAllowed("ADMIN")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @GET
    public List<UserResponse> listarTodos() {
        return UserEntity.<UserEntity>listAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @GET
    @Path("/nome/{nome}")
    public List<UserResponse> buscarPorNome(@PathParam("nome") String nome) {
        return UserEntity.buscarPorNome(nome)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @GET
    @Path("/cpf/{cpf}")
    public UserResponse buscarPorCpf(@PathParam("cpf") String cpf) {
        UserEntity user = UserEntity.buscarPorCpf(cpf)
                .orElseThrow(() -> erro404("Usuário não encontrado para o CPF informado"));

        return UserResponse.from(user);
    }

    @GET
    @Path("/telefone/{telefone}")
    public UserResponse buscarPorTelefone(@PathParam("telefone") String telefone) {
        UserEntity user = UserEntity.buscarPorTelefone(telefone)
                .orElseThrow(() -> erro404("Usuário não encontrado para o telefone informado"));

        return UserResponse.from(user);
    }

    @POST
    @Transactional
    @PermitAll
    public Response cadastrar(UserRequest request) {
        if (UserEntity.existeCpf(request.cpf())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Já existe um usuário cadastrado com este CPF"))
                    .build();
        }

        if (UserEntity.existeUsername(request.username())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(new ErrorResponse("Já existe um usuário cadastrado com este username"))
                    .build();
        }

        UserEntity user = new UserEntity();
        user.nome = request.nome();
        user.cpf = request.cpf();
        user.endereco = request.endereco();
        user.email = request.email();
        user.telefone = request.telefone();
        user.username = request.username();
        user.passwordHash = BcryptUtil.bcryptHash(request.password());

        // Cadastro público sempre cria usuário comum
        user.role = "USER";

        user.avatar = request.avatar();
        user.bio = request.bio();

        user.persist();

        return Response.status(Response.Status.CREATED)
                .entity(UserResponse.from(user))
                .build();
    }

    @PUT
    @Path("/{cpf}")
    @Transactional
    public UserResponse editar(
            @PathParam("cpf") String cpf,
            UserRequest request
    ) {
        UserEntity user = UserEntity.buscarPorCpf(cpf)
                .orElseThrow(() -> erro404("Usuário não encontrado para edição"));

        user.nome = request.nome();
        user.endereco = request.endereco();
        user.email = request.email();
        user.telefone = request.telefone();
        user.avatar = request.avatar();
        user.bio = request.bio();

        if (request.role() != null) {
            user.role = request.role();
        }

        return UserResponse.from(user);
    }

    @DELETE
    @Path("/{cpf}")
    @Transactional
    public Response apagar(@PathParam("cpf") String cpf) {
        UserEntity user = UserEntity.buscarPorCpf(cpf)
                .orElseThrow(() -> erro404("Usuário não encontrado para exclusão"));

        user.delete();

        return Response.noContent().build();
    }

    private NotFoundException erro404(String mensagem) {
        return new NotFoundException(
                Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse(mensagem))
                        .build()
        );
    }
}