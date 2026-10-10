package com.example.store.service.auth;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.example.store.dto.auth.GoogleUserInfo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleAuthService {

    @Value("${google.client.id:}")
    private String googleClientId;

    private final RestClient restClient;

    public GoogleAuthService() {
        this.restClient = RestClient.builder().build();
    }

    public String getGoogleClientId() {
        return googleClientId != null ? googleClientId.trim() : "";
    }

    public GoogleUserInfo verifyToken(String idToken, String accessToken) {
        if ((idToken == null || idToken.isBlank()) && (accessToken == null || accessToken.isBlank())) {
            throw new RuntimeException("Google token cannot be empty!");
        }

        try {
            Map<String, Object> payload;

            if (accessToken != null && !accessToken.isBlank()) {
                // Verify via Google OAuth2 UserInfo API with Bearer token
                payload = restClient.get()
                        .uri("https://www.googleapis.com/oauth2/v3/userinfo")
                        .header("Authorization", "Bearer " + accessToken.trim())
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});
            } else {
                // Verify via Google OAuth2 TokenInfo API with ID Token
                payload = restClient.get()
                        .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken.trim())
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {});

                // Verify audience if configured
                String aud = (String) payload.get("aud");
                String configuredClientId = getGoogleClientId();
                if (!configuredClientId.isEmpty() && !configuredClientId.equalsIgnoreCase("your_google_client_id_here")) {
                    if (!configuredClientId.equals(aud)) {
                        log.error("Google token audience mismatch: expected [{}], received [{}]", configuredClientId, aud);
                        throw new RuntimeException("Google token client ID does not match server configuration!");
                    }
                }
            }

            if (payload == null || payload.containsKey("error")) {
                String error = payload != null ? String.valueOf(payload.get("error_description")) : "Unknown error";
                log.error("Google token verification failed with error: {}", error);
                throw new RuntimeException("Invalid Google token: " + error);
            }

            String email = (String) payload.get("email");
            String emailVerified = String.valueOf(payload.get("email_verified"));
            if (email == null || !"true".equalsIgnoreCase(emailVerified)) {
                throw new RuntimeException("Google account email is not verified!");
            }

            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");
            String sub = (String) payload.get("sub");

            return GoogleUserInfo.builder()
                    .sub(sub)
                    .email(email.toLowerCase().trim())
                    .name(name != null && !name.isBlank() ? name : email)
                    .picture(picture)
                    .build();

        } catch (Exception e) {
            log.error("Error verifying Google token: {}", e.getMessage());
            throw new RuntimeException("Google authentication failed: " + e.getMessage());
        }
    }
}
