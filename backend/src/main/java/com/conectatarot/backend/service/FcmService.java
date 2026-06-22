package com.conectatarot.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends FCM push notifications via the FCM HTTP v1 API.
 * Requires FIREBASE_CREDENTIALS env var: base64-encoded service account JSON.
 * Uses google-auth-library (already transitive via google-api-client) — no gRPC.
 * If the env var is absent, notifications are silently skipped.
 */
@Service
public class FcmService {

    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final AtomicReference<GoogleCredentials> credentialsRef = new AtomicReference<>();
    private volatile String projectId;

    private GoogleCredentials getCredentials() {
        if (credentialsRef.get() != null) return credentialsRef.get();
        synchronized (this) {
            if (credentialsRef.get() != null) return credentialsRef.get();
            String encoded = System.getenv("FIREBASE_CREDENTIALS");
            if (encoded == null || encoded.isBlank()) return null;
            try {
                byte[] json = Base64.getDecoder().decode(encoded.strip());
                GoogleCredentials creds = GoogleCredentials
                        .fromStream(new ByteArrayInputStream(json))
                        .createScoped(Collections.singletonList(FCM_SCOPE));
                projectId = extractProjectId(new String(json, StandardCharsets.UTF_8));
                credentialsRef.set(creds);
                return creds;
            } catch (Exception e) {
                System.err.println("FcmService: failed to load credentials: " + e.getMessage());
                return null;
            }
        }
    }

    public void sendNotification(String fcmToken, String title, String body, String tipo) {
        if (fcmToken == null || fcmToken.isBlank()) return;
        GoogleCredentials creds = getCredentials();
        if (creds == null || projectId == null) return;

        try {
            creds.refreshIfExpired();
            String accessToken = creds.getAccessToken().getTokenValue();
            String fcmUrl = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";
            String payload = buildPayload(fcmToken, title, body, tipo);

            HttpURLConnection conn = (HttpURLConnection) new URL(fcmUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("FcmService: HTTP " + code + " sending notification");
            }
        } catch (Exception e) {
            System.err.println("FcmService: error sending notification: " + e.getMessage());
        }
    }

    private String buildPayload(String token, String title, String body, String tipo) {
        return String.format(
            "{\"message\":{\"token\":\"%s\",\"notification\":{\"title\":\"%s\",\"body\":\"%s\"}," +
            "\"data\":{\"tipo\":\"%s\",\"title\":\"%s\",\"body\":\"%s\"}}}",
            esc(token), esc(title), esc(body), esc(tipo), esc(title), esc(body)
        );
    }

    private String extractProjectId(String json) {
        int idx = json.indexOf("\"project_id\"");
        if (idx < 0) return null;
        int start = json.indexOf('"', idx + 13) + 1;
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
