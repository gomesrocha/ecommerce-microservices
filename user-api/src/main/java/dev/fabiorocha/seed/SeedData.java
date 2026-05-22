package dev.fabiorocha.seed;

import dev.fabiorocha.user.entity.UserEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;

@ApplicationScoped
public class SeedData {

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (UserEntity.buscarPorUsername("admin").isPresent()) {
            return;
        }

        UserEntity admin = new UserEntity();
        admin.nome = "Administrador";
        admin.cpf = "999";
        admin.endereco = "Rua Admin";
        admin.email = "admin@email.com";
        admin.telefone = "9999";
        admin.username = "admin";
        admin.passwordHash = BcryptUtil.bcryptHash("admin123");
        admin.role = "ADMIN";
        admin.ativo = true;
        admin.dataCadastro = LocalDateTime.now();

        admin.persist();
    }
}