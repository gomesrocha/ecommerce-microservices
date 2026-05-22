package dev.fabiorocha.profile;

public record ProfileUpdateRequest(
        String nome,
        String endereco,
        String telefone,
        String avatar,
        String bio
) {
}