package com.horovod.hub.service;

import com.horovod.hub.config.HorovodProperties;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final long OTP_TTL_SECONDS = 600;

    private final HorovodProperties properties;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();
    private final JwtService jwtService;

    public AuthService(HorovodProperties properties, JwtService jwtService) {
        this.properties = properties;
        this.jwtService = jwtService;
    }

    public Map<String, Object> getSession(String token) {
        return jwtService.verifyToken(token)
                .filter(jwt -> "access".equals(jwtService.getTokenType(jwt)))
                .map(jwt -> {
                    String email = jwtService.getEmailFromToken(jwt);
                    boolean isAdmin = jwtService.isAdmin(jwt);
                    return Map.<String, Object>of(
                            "session", Map.of(
                                    "access_token", token,
                                    "user", Map.of("email", email, "isAdmin", isAdmin)
                            )
                    );
                })
                .orElseGet(() -> {
                    java.util.Map<String, Object> result = new java.util.HashMap<>();
                    result.put("session", null);
                    return result;
                });
    }

    public Optional<Map<String, Object>> refreshToken(String refreshToken) {
        return jwtService.verifyToken(refreshToken)
                .filter(jwt -> "refresh".equals(jwtService.getTokenType(jwt)))
                .map(jwt -> {
                    String email = jwtService.getEmailFromToken(jwt);
                    boolean isAdmin = properties.isAdminEmail(email); // Re-evaluate admin status
                    String newAccessToken = jwtService.generateAccessToken(email, isAdmin);
                    String newRefreshToken = jwtService.generateRefreshToken(email);

                    return Map.<String, Object>of(
                            "access_token", newAccessToken,
                            "refresh_token", newRefreshToken,
                            "user", Map.of("email", email, "isAdmin", isAdmin)
                    );
                });
    }

    public Optional<Map<String, Object>> loginWithPassword(String email, String password) {
        if (!properties.isAdminEmail(email)) {
            return Optional.empty();
        }
        if (!properties.getAdminPasswordFallback().equals(password)) {
            return Optional.empty();
        }

        String accessToken = jwtService.generateAccessToken(email, true);
        String refreshToken = jwtService.generateRefreshToken(email);

        return Optional.of(Map.of(
                "user", Map.of("email", email, "isAdmin", true),
                "session", Map.of(
                        "access_token", accessToken,
                        "refresh_token", refreshToken,
                        "user", Map.of("email", email, "isAdmin", true)
                )
        ));
    }

    public void sendOtp(String email) {
        if (properties.isAdminEmail(email)) {
            return;
        }
        String code = String.format("%04d", random.nextInt(10000));
        otpStore.put(email.toLowerCase(), new OtpRecord(code, Instant.now().getEpochSecond()));
        // Dev: OTP logged to console (no email provider wired yet)
        System.out.println("[HOROVOD OTP] Email: " + email + " Code: " + code);
    }

    public Optional<Map<String, Object>> verifyOtp(String email, String token) {
        String key = email.toLowerCase();
        if (properties.isOtpBypass(token)) {
            return Optional.of(createSessionMap(email, false));
        }

        OtpRecord record = otpStore.get(key);
        if (record == null) {
            return Optional.empty();
        }
        long now = Instant.now().getEpochSecond();
        if (now - record.createdAt() > OTP_TTL_SECONDS) {
            otpStore.remove(key);
            return Optional.empty();
        }
        if (!record.code().equals(token.trim())) {
            return Optional.empty();
        }
        otpStore.remove(key);

        boolean isAdmin = properties.isAdminEmail(email);
        return Optional.of(createSessionMap(email, isAdmin));
    }

    public void logout(String sessionId) {
        // Stateless JWT logout requires frontend to discard tokens.
        // For blacklisting, a distributed cache like Redis would be needed.
    }

    private Map<String, Object> createSessionMap(String email, boolean admin) {
        String accessToken = jwtService.generateAccessToken(email, admin);
        String refreshToken = jwtService.generateRefreshToken(email);

        return Map.of(
                "session", Map.of(
                        "access_token", accessToken,
                        "refresh_token", refreshToken,
                        "user", Map.of("email", email, "isAdmin", admin)
                )
        );
    }

    private record OtpRecord(String code, long createdAt) {
    }
}
