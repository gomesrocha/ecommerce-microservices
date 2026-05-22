package dev.fabiorocha.user.dto;

public record UserRequest(
        String nome,
        String cpf,
        String endereco,
        String email,
        String telefone,
        String username,
        String password,
        String role,
        String avatar,
        String bio
) {
}