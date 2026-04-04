package com.bitiriciler32.cms.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = resolveCredentials(credentialsPath).getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully from: {}", credentialsPath);
            }
        } catch (IOException e) {
            log.warn("Firebase credentials not found at '{}'. Push notifications will be disabled.", credentialsPath);
        }
    }

    /**
     * Resolves the credentials path to a Spring Resource.
     * <ul>
     *   <li>Paths starting with {@code file:} are treated as filesystem paths
     *       (e.g. a volume-mounted file in production).</li>
     *   <li>All other paths are resolved against the classpath
     *       (convenient for local development).</li>
     * </ul>
     * Example .env values:
     * <pre>
     *   # local dev  – file lives in src/main/resources/
     *   FIREBASE_CREDENTIALS_PATH=firebase-service-account.json
     *
     *   # production – file is volume-mounted into the container
     *   FIREBASE_CREDENTIALS_PATH=file:/app/config/firebase-service-account.json
     * </pre>
     */
    private Resource resolveCredentials(String path) {
        if (path.startsWith("file:")) {
            return new FileSystemResource(path.substring("file:".length()));
        }
        return new ClassPathResource(path);
    }
}
