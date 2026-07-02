package com.horovod.hub.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.horovod.hub.config.HorovodProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService(HorovodProperties properties) {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            secret = "default_fallback_secret_key_that_is_at_least_32_characters_long";
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm)
                .withIssuer("horovod-hub")
                .build();
    }

    public String generateAccessToken(String email, boolean isAdmin) {
        return JWT.create()
                .withIssuer("horovod-hub")
                .withSubject(email)
                .withClaim("isAdmin", isAdmin)
                .withClaim("token_type", "access")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                .sign(algorithm);
    }

    public String generateRefreshToken(String email) {
        return JWT.create()
                .withIssuer("horovod-hub")
                .withSubject(email)
                .withClaim("token_type", "refresh")
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                .sign(algorithm);
    }

    public Optional<DecodedJWT> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(verifier.verify(token));
        } catch (JWTVerificationException e) {
            return Optional.empty();
        }
    }

    public String getEmailFromToken(DecodedJWT jwt) {
        return jwt.getSubject();
    }

    public boolean isAdmin(DecodedJWT jwt) {
        return Boolean.TRUE.equals(jwt.getClaim("isAdmin").asBoolean());
    }

    public String getTokenType(DecodedJWT jwt) {
        return jwt.getClaim("token_type").asString();
    }
}
