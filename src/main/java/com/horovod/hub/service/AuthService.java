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
    private final Map<String, UserSession> sessions = new ConcurrentHashMap<>();

    public AuthService(HorovodProperties properties) {
        this.properties = properties;
    }

    public Map<String, Object> getSession(String sessionId) {
        if (sessionId == null) {
            return Map.of("session", null);
        }
        UserSession session = sessions.get(sessionId);
        if (session == null) {
            return Map.of("session", null);
        }
        return Map.of("session", session.toMap());
    }

    public Optional<Map<String, Object>> loginWithPassword(String email, String password) {
        if (!properties.isAdminEmail(email)) {
            return Optional.empty();
        }
        if (!properties.getAdminPasswordFallback().equals(password)) {
            return Optional.empty();
        }
        UserSession session = createSession(email, true);
        return Optional.of(Map.of(
                "user", Map.of("email", email),
                "session", session.toMap()
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
            UserSession session = createSession(email, false);
            return Optional.of(Map.of("session", session.toMap()));
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
        UserSession session = createSession(email, false);
        return Optional.of(Map.of("session", session.toMap()));
    }

    public void logout(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }

    private UserSession createSession(String email, boolean admin) {
        String id = "sess_" + System.currentTimeMillis() + "_" + random.nextInt(100000);
        UserSession session = new UserSession(id, email, admin);
        sessions.put(id, session);
        return session;
    }

    private record OtpRecord(String code, long createdAt) {
    }

    public static final class UserSession {
        private final String id;
        private final String email;
        private final boolean admin;

        public UserSession(String id, String email, boolean admin) {
            this.id = id;
            this.email = email;
            this.admin = admin;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "access_token", id,
                    "user", Map.of("email", email, "isAdmin", admin)
            );
        }
    }
}
