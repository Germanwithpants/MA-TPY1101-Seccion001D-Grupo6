package com.conectatarot.backend.service;

import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Sends FCM push notifications via the FCM HTTP v1 API.
 * Requires FIREBASE_SERVER_KEY environment variable (Legacy HTTP API key).
 * If not set, notifications are silently skipped — the app still works.
 */
@Service
public class FcmService {

    private static final String FCM_URL = "https://fcm.googleapis.com/fcm/send";

    public void sendNotification(String fcmToken, String title, String body, String tipo) {
        if (fcmToken == null || fcmToken.isBlank()) return;

        String serverKey = System.getenv("FIREBASE_SERVER_KEY");
        if (serverKey == null || serverKey.isBlank()) return;

        try {
            String payload = String.format(
                "{\"to\":\"%s\",\"notification\":{\"title\":\"%s\",\"body\":\"%s\",\"sound\":\"default\"}," +
                "\"data\":{\"tipo\":\"%s\",\"title\":\"%s\",\"body\":\"%s\"}}",
                fcmToken, escapeJson(title), escapeJson(body), tipo, escapeJson(title), escapeJson(body)
            );

            HttpURLConnection conn = (HttpURLConnection) new URL(FCM_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "key=" + serverKey);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                System.err.println("FcmService: HTTP " + code + " al enviar notificación a " + fcmToken.substring(0, 10) + "...");
            }
        } catch (Exception e) {
            System.err.println("FcmService: error enviando notificación: " + e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
