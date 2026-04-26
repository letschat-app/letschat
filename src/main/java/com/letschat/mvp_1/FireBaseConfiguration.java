package com.letschat.mvp_1;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

@Configuration
public class FireBaseConfiguration {

    @Value("${firebase.config}")
    private String firebaseConfig;

    @PostConstruct
    public void init() {
        try {
            if (firebaseConfig == null || firebaseConfig.isEmpty()) {
                throw new RuntimeException("firebase.config not set");
            }

            // Fix newline issue from env
            firebaseConfig = firebaseConfig.replace("\\n", "\n");

            InputStream serviceAccount = new ByteArrayInputStream(
                    firebaseConfig.getBytes(StandardCharsets.UTF_8)
            );

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

        } catch (Exception e) {
            throw new RuntimeException("Firebase init failed", e);
        }
    }
}