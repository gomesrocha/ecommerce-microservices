package dev.fabiorocha.auth;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Path("/auth/.well-known")
@PermitAll
@Produces(MediaType.APPLICATION_JSON)
public class JwksResource {

    public static final String KEY_ID = "ecommerce-key-1";

    @GET
    @Path("/jwks.json")
    public Map<String, Object> jwks() {
        RSAPublicKey publicKey = loadPublicKey();

        Map<String, Object> jwk = Map.of(
                "kty", "RSA",
                "use", "sig",
                "kid", KEY_ID,
                "alg", "RS256",
                "n", base64UrlUnsigned(publicKey.getModulus()),
                "e", base64UrlUnsigned(publicKey.getPublicExponent())
        );

        return Map.of("keys", List.of(jwk));
    }

    private RSAPublicKey loadPublicKey() {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("publicKey.pem")) {

            if (inputStream == null) {
                throw new IllegalStateException("publicKey.pem não encontrado em src/main/resources");
            }

            String pem = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(pem);

            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao carregar publicKey.pem", exception);
        }
    }

    private String base64UrlUnsigned(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            byte[] unsigned = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, unsigned, 0, unsigned.length);
            bytes = unsigned;
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}