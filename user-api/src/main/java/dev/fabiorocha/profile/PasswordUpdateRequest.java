package dev.fabiorocha.profile;

public record PasswordUpdateRequest(
        String senhaAtual,
        String novaSenha,
        String confirmarNovaSenha
) {
}