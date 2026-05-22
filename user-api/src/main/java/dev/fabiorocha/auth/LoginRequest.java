package dev.fabiorocha.auth;

public record LoginRequest(
        String username,
        String password
) {
}