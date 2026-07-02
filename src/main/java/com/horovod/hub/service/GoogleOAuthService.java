package com.horovod.hub.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.horovod.hub.config.HorovodProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@Service
public class GoogleOAuthService {

    private final HorovodProperties properties;
    private final RestTemplate restTemplate;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, StateRecord> stateStore = new ConcurrentHashMap<>();

    private static final long STATE_TTL_SECONDS = 300; // 5 minutes

    public GoogleOAuthService(HorovodProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.jwtService = jwtService;
    }

    public String generateAuthUrl(String frontendRedirectUri) {
        cleanExpiredStates();
        String state = generateState();
        stateStore.put(state, new StateRecord(frontendRedirectUri, Instant.now().getEpochSecond()));

        String clientId = properties.getOauth().getGoogle().getClientId();

        try {
            return String.format(
                "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=openid%%20email%%20profile" +
                "&state=%s",
                clientId, java.net.URLEncoder.encode(frontendRedirectUri, java.nio.charset.StandardCharsets.UTF_8.toString()), state
            );
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 not supported", e);
        }
    }

    public Optional<Map<String, Object>> processCallback(String code, String state, String frontendRedirectUri) {
        StateRecord record = stateStore.remove(state);
        if (record == null) {
            return Optional.empty(); // Invalid state
        }
        if (Instant.now().getEpochSecond() - record.createdAt() > STATE_TTL_SECONDS) {
            return Optional.empty(); // State expired
        }

        if (!record.redirectUri().equals(frontendRedirectUri)) {
            return Optional.empty(); // Redirect URI mismatch
        }

        String clientId = properties.getOauth().getGoogle().getClientId();
        String clientSecret = properties.getOauth().getGoogle().getClientSecret();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", frontendRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", request, GoogleTokenResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String idToken = response.getBody().idToken();
                String email = extractEmailFromIdToken(idToken);

                if (email != null) {
                    boolean isAdmin = properties.isAdminEmail(email);
                    String accessToken = jwtService.generateAccessToken(email, isAdmin);
                    String refreshToken = jwtService.generateRefreshToken(email);

                    return Optional.of(Map.of(
                            "access_token", accessToken,
                            "refresh_token", refreshToken,
                            "user", Map.of("email", email, "isAdmin", isAdmin)
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private void cleanExpiredStates() {
        long now = Instant.now().getEpochSecond();
        stateStore.entrySet().removeIf(entry ->
                now - entry.getValue().createdAt() > STATE_TTL_SECONDS);
    }

    private String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String extractEmailFromIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length == 3) {
                // Since java-jwt verifier will fail on signature (we don't have google's certs here),
                // we must decode without verifying the signature.
                var decoded = com.auth0.jwt.JWT.decode(idToken);
                return decoded.getClaim("email").asString();
            }
        } catch (Exception e) {
             e.printStackTrace();
        }
        return null;
    }

    private record StateRecord(String redirectUri, long createdAt) {}

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Integer expiresIn,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("scope") String scope,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("refresh_token") String refreshToken
    ) {}
}
