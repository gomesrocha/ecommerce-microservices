package dev.fabiorocha.user.dto;


import dev.fabiorocha.user.entity.UserEntity;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String nome,
        String cpf,
        String endereco,
        String email,
        String telefone,
        String username,
        String role,
        String avatar,
        String bio,
        Boolean ativo,
        LocalDateTime dataCadastro
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(
                user.id,
                user.nome,
                user.cpf,
                user.endereco,
                user.email,
                user.telefone,
                user.username,
                user.role,
                user.avatar,
                user.bio,
                user.ativo,
                user.dataCadastro
        );
    }
}