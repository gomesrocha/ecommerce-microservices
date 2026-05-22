package dev.fabiorocha.user.entity;


import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntity {

    @Column(nullable = false)
    public String nome;

    @Column(nullable = false, unique = true)
    public String cpf;

    public String endereco;

    @Column(nullable = false, unique = true)
    public String email;

    public String telefone;

    @Column(nullable = false, unique = true)
    public String username;

    @Column(nullable = false)
    public String passwordHash;

    @Column(nullable = false)
    public String role = "USER";

    public String avatar;

    public String bio;

    @Column(nullable = false)
    public Boolean ativo = true;

    @Column(nullable = false)
    public LocalDateTime dataCadastro = LocalDateTime.now();

    public static List<UserEntity> buscarPorNome(String nome) {
        return list("lower(nome) like lower(?1)", "%" + nome + "%");
    }

    public static Optional<UserEntity> buscarPorCpf(String cpf) {
        return find("cpf", cpf).firstResultOptional();
    }

    public static Optional<UserEntity> buscarPorTelefone(String telefone) {
        return find("telefone", telefone).firstResultOptional();
    }

    public static Optional<UserEntity> buscarPorUsername(String username) {
        return find("username", username).firstResultOptional();
    }

    public static boolean existeCpf(String cpf) {
        return count("cpf", cpf) > 0;
    }

    public static boolean existeUsername(String username) {
        return count("username", username) > 0;
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }
}