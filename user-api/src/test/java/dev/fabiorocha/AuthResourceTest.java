package dev.fabiorocha;


import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AuthResourceTest {

    @Test
    void deveFazerLoginComSucesso() {
        String usuarioJson = """
                {
                    "nome": "Admin",
                    "cpf": "999",
                    "endereco": "Rua Admin",
                    "email": "admin@email.com",
                    "telefone": "9999",
                    "username": "admin",
                    "password": "admin123",
                    "role": "ADMIN"
                }
                """;

        given()
                .contentType("application/json")
                .body(usuarioJson)
                .when()
                .post("/users")
                .then()
                .statusCode(anyOf(is(201), is(409)));

        String loginJson = """
                {
                    "username": "admin",
                    "password": "admin123"
                }
                """;

        given()
                .contentType("application/json")
                .body(loginJson)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .body("access_token", notNullValue())
                .body("refresh_token", notNullValue())
                .body("token_type", equalTo("bearer"));
    }

    @Test
    void deveRetornar401QuandoLoginForInvalido() {
        String loginJson = """
                {
                    "username": "admin",
                    "password": "senha-errada"
                }
                """;

        given()
                .contentType("application/json")
                .body(loginJson)
                .when()
                .post("/auth/login")
                .then()
                .statusCode(401)
                .body("mensagem", equalTo("Usuário ou senha inválidos"));
    }
}