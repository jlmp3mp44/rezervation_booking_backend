package com.horovod.hub.controller;

import com.horovod.hub.config.HorovodProperties;
import com.horovod.hub.service.AuthService;
import com.horovod.hub.service.GoogleOAuthService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${horovod.cors-origins:http://localhost:3000,http://127.0.0.1:3000,https://horovod.sk}", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final HorovodProperties properties;
    private final GoogleOAuthService googleOAuthService;

    public AuthController(AuthService authService, HorovodProperties properties, GoogleOAuthService googleOAuthService) {
        this.authService = authService;
        this.properties = properties;
        this.googleOAuthService = googleOAuthService;
    }

    @GetMapping("/google/url")
    public Map<String, String> getGoogleAuthUrl(@RequestParam("redirect_uri") String redirectUri) {
        String url = googleOAuthService.generateAuthUrl(redirectUri);
        return Map.of("url", url);
    }

    @PostMapping("/google")
    public ResponseEntity<Map<String, Object>> processGoogleAuth(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String state = body.get("state");
        String redirectUri = body.get("redirect_uri");

        return googleOAuthService.processCallback(code, state, redirectUri)
                .<ResponseEntity<Map<String, Object>>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Google authentication failed!")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        return authService.refreshToken(refreshToken)
                .<ResponseEntity<Map<String, Object>>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Невірний або прострочений токен оновлення!")));
    }

    @GetMapping("/session")
    public Map<String, Object> getSession(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = extractToken(authHeader);
        return authService.getSession(token);
    }

    @PostMapping("/login/password")
    public ResponseEntity<Map<String, Object>> loginWithPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        return authService.loginWithPassword(email, password)
                .<ResponseEntity<Map<String, Object>>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Невірний пароль адміністратора!")));
    }

    @PostMapping("/otp/send")
    public Map<String, Object> sendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        authService.sendOtp(email);
        return Map.of("success", true);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String token = body.get("token");
        return authService.verifyOtp(email, token)
                .<ResponseEntity<Map<String, Object>>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Невірний код підтвердження!")));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(extractToken(authHeader));
        return Map.of("success", true);
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
