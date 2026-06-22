package com.conectatarot.backend.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends FCM push notifications via the FCM HTTP v1 API using Firebase Admin SDK.
 * Requires FIREBASE_CREDENTIALS env var: base64-encoded service account JSON.
 * If not set, notifications are silently skipped — the app still works without it.
 */
@Service
public class FcmService {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private boolean init() {
        if (initialized.get()) return true;
        synchronized (this) {
            if (initialized.get()) return true;
            String credentials = System.getenv("FIREBASE_CREDENTIALS");
            if (credentials == null || credentials.isBlank()) return false;
            try {
                byte[] json = Base64.getDecoder().decode(credentials.strip());
                GoogleCredentials googleCredentials = GoogleCredentials
                        .fromStream(new ByteArrayInputStream(json))
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(googleCredentials)
                        .build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                initialized.set(true);
                return true;
            } catch (Exception e) {
                System.err.println("FcmService: failed to initialize Firebase: " + e.getMessage());
                return false;
            }
        }
    }

    public void sendNotification(String fcmToken, String title, String body, String tipo) {
        if (fcmToken == null || fcmToken.isBlank()) return;
        if (!init()) return;

        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("tipo", tipo)
                    .putData("title", title != null ? title : "")
                    .putData("body", body != null ? body : "")
                    .build();

            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            System.err.println("FcmService: error sending notification: " + e.getMessage());
        }
    }
}
