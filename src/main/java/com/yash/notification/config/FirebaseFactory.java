package com.yash.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@Factory
public class FirebaseFactory {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseFactory.class);

    private final FirebaseConfig firebaseConfig;

    public FirebaseFactory(FirebaseConfig firebaseConfig) {
        this.firebaseConfig = firebaseConfig;
    }

    @Singleton
    public FirebaseApp firebaseApp() throws IOException {
        String serviceAccountPath = firebaseConfig.getServiceAccountKeyPath();
        if (serviceAccountPath == null || !serviceAccountPath.startsWith("classpath:")) {
            throw new IOException("Firebase service account key path not configured properly in application.yml. It should be a classpath resource.");
        }

        String path = serviceAccountPath.substring("classpath:".length());
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream(path);

        if (serviceAccount == null) {
            LOG.error("Could not find Firebase service account key file at: {}", serviceAccountPath);
            throw new IOException("Resource not found: " + serviceAccountPath);
        }

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

        if (FirebaseApp.getApps().isEmpty()) {
            LOG.info("Initializing Firebase app...");
            return FirebaseApp.initializeApp(options);
        } else {
            LOG.info("Firebase app already initialized.");
            return FirebaseApp.getInstance();
        }
    }

    @Singleton
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
} 