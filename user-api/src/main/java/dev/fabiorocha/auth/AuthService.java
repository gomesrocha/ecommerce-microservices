package dev.fabiorocha.auth;


import dev.fabiorocha.user.entity.UserEntity;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class AuthService {

    @ConfigProperty(name = "app.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "app.jwt.access-token-minutes")
    long accessTokenMinutes;

    @ConfigProperty(name = "app.jwt.refresh-token-minutes")
    long refreshTokenMinutes;

    public Optional<UserEntity> autenticar(String username, String password) {
        return UserEntity.buscarPorUsername(username)
                .filter(user -> Boolean.TRUE.equals(user.ativo))
                .filter(user -> BcryptUtil.matches(password, user.passwordHash));
    }

    public TokenResponse gerarTokens(UserEntity user, boolean fresh) {
        String accessToken = gerarToken(user, "access_token", accessTokenMinutes, fresh);
        String refreshToken = gerarToken(user, "refresh_token", refreshTokenMinutes, false);

        return new TokenResponse(accessToken, refreshToken, "bearer");
    }

    private String gerarToken(
            UserEntity user,
            String tipo,
            long minutos,
            boolean fresh
    ) {
        return Jwt.issuer(issuer)
                .subject(user.username)
                .upn(user.username)
                .groups(Set.of(user.role))
                .claim("tipo", tipo)
                .claim("fresh", fresh)
                .claim("cpf", user.cpf)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(minutos)))
                .jws()
                .keyId(JwksResource.KEY_ID)
                .sign();
    }
}