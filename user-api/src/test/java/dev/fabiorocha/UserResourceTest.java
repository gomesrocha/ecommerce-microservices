package dev.fabiorocha;

import dev.fabiorocha.user.entity.UserEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserResourceTest {

    @BeforeEach
    @Transactional
    void prepararTeste() {
        UserEntity.deleteAll();

        criarUsuario(
                "Fabio",
                "123",
                "rua 1",
                "fabio@fabio.com",
                "9999",
                "fabio",
                "123456",
                "USER"
        );

        criarUsuario(
                "Rosi",
                "456",
                "rua 2",
                "rosi@email.com",
                "8888",
                "rosi",
                "123456",
                "USER"
        );

        criarUsuario(
                "Camila",
                "789",
                "rua 3",
                "camila@email.com",
                "7777",
                "camila",
                "123456",
                "USER"
        );
    }

    private void criarUsuario(
            String nome,
            String cpf,
            String endereco,
            String email,
            String telefone,
            String username,
            String password,
            String role
    ) {
        UserEntity user = new UserEntity();
        user.nome = nome;
        user.cpf = cpf;
        user.endereco = endereco;
        user.email = email;
        user.telefone = telefone;
        user.username = username;
        user.passwordHash = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.ativo = true;
        user.dataCadastro = LocalDateTime.now();

        user.persist();
    }

    @Test
    void deveListarTodosOsUsuarios() {
        given()
                .when()
                .get("/users")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(3))
                .body("nome", hasItems("Fabio", "Rosi", "Camila"))
                .body("cpf", hasItems("123", "456", "789"));
    }

    @Test
    void deveBuscarUsuarioPorNome() {
        given()
                .when()
                .get("/users/nome/Fabio")
                .then()
                .statusCode(200)
                .body("size()", greaterThanOrEqualTo(1))
                .body("[0].nome", equalTo("Fabio"));
    }

    @Test
    void deveBuscarUsuarioPorCpf() {
        given()
                .when()
                .get("/users/cpf/123")
                .then()
                .statusCode(200)
                .body("nome", equalTo("Fabio"))
                .body("cpf", equalTo("123"))
                .body("email", equalTo("fabio@fabio.com"))
                .body("username", equalTo("fabio"));
    }

    @Test
    void deveRetornar404QuandoCpfNaoExistir() {
        given()
                .when()
                .get("/users/cpf/000")
                .then()
                .statusCode(404)
                .body("mensagem", equalTo("Usuário não encontrado para o CPF informado"));
    }

    @Test
    void deveBuscarUsuarioPorTelefone() {
        given()
                .when()
                .get("/users/telefone/9999")
                .then()
                .statusCode(200)
                .body("nome", equalTo("Fabio"))
                .body("telefone", equalTo("9999"));
    }

    @Test
    void deveCadastrarNovoUsuario() {
        String novoUsuarioJson = """
                {
                    "nome": "João",
                    "cpf": "111",
                    "endereco": "Rua Nova",
                    "email": "joao@email.com",
                    "telefone": "5555",
                    "username": "joao",
                    "password": "123456",
                    "role": "USER"
                }
                """;

        given()
                .contentType("application/json")
                .body(novoUsuarioJson)
                .when()
                .post("/users")
                .then()
                .statusCode(201)
                .body("nome", equalTo("João"))
                .body("cpf", equalTo("111"))
                .body("email", equalTo("joao@email.com"))
                .body("telefone", equalTo("5555"))
                .body("username", equalTo("joao"))
                .body("dataCadastro", notNullValue());
    }

    @Test
    void deveRetornar409QuandoCpfJaExistir() {
        String usuarioDuplicadoJson = """
                {
                    "nome": "Outro Fabio",
                    "cpf": "123",
                    "endereco": "Outra Rua",
                    "email": "outro@email.com",
                    "telefone": "0000",
                    "username": "outro-fabio",
                    "password": "123456",
                    "role": "USER"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioDuplicadoJson)
                .when()
                .post("/users")
                .then()
                .statusCode(409)
                .body("mensagem", equalTo("Já existe um usuário cadastrado com este CPF"));
    }

    @Test
    void deveRetornar409QuandoUsernameJaExistir() {
        String usuarioDuplicadoJson = """
                {
                    "nome": "Outro Usuário",
                    "cpf": "999",
                    "endereco": "Outra Rua",
                    "email": "outro@email.com",
                    "telefone": "0000",
                    "username": "fabio",
                    "password": "123456",
                    "role": "USER"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioDuplicadoJson)
                .when()
                .post("/users")
                .then()
                .statusCode(409)
                .body("mensagem", equalTo("Já existe um usuário cadastrado com este username"));
    }

    @Test
    void deveEditarUsuarioExistente() {
        String usuarioAtualizadoJson = """
                {
                    "nome": "Fabio Atualizado",
                    "cpf": "123",
                    "endereco": "Rua Atualizada",
                    "email": "fabio.atualizado@email.com",
                    "telefone": "1010",
                    "username": "fabio",
                    "password": "123456",
                    "role": "USER",
                    "avatar": "https://example.com/avatar.png",
                    "bio": "Bio atualizada"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioAtualizadoJson)
                .when()
                .put("/users/123")
                .then()
                .statusCode(200)
                .body("nome", equalTo("Fabio Atualizado"))
                .body("cpf", equalTo("123"))
                .body("endereco", equalTo("Rua Atualizada"))
                .body("email", equalTo("fabio.atualizado@email.com"))
                .body("telefone", equalTo("1010"))
                .body("avatar", equalTo("https://example.com/avatar.png"))
                .body("bio", equalTo("Bio atualizada"));
    }

    @Test
    void deveRetornar404AoEditarUsuarioInexistente() {
        String usuarioAtualizadoJson = """
                {
                    "nome": "Usuário Inexistente",
                    "cpf": "000",
                    "endereco": "Rua X",
                    "email": "inexistente@email.com",
                    "telefone": "0000",
                    "username": "inexistente",
                    "password": "123456",
                    "role": "USER"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioAtualizadoJson)
                .when()
                .put("/users/000")
                .then()
                .statusCode(404)
                .body("mensagem", equalTo("Usuário não encontrado para edição"));
    }

    @Test
    void deveApagarUsuarioExistente() {
        String usuarioJson = """
                {
                    "nome": "Usuário Para Apagar",
                    "cpf": "222",
                    "endereco": "Rua Delete",
                    "email": "delete@email.com",
                    "telefone": "2222",
                    "username": "delete",
                    "password": "123456",
                    "role": "USER"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioJson)
                .when()
                .post("/users")
                .then()
                .statusCode(201);

        given()
                .when()
                .delete("/users/222")
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/users/cpf/222")
                .then()
                .statusCode(404);
    }
}