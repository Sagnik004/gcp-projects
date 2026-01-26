package com.sagnikchakraborty.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class BigQueryConfig {

    private final ResourceLoader resourceLoader;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.credentials.location}")
    private String credentialsLocation;

    public BigQueryConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    public BigQuery bigQuery() throws IOException {
        BigQueryOptions.Builder builder = BigQueryOptions.newBuilder()
                .setProjectId(projectId);

        // Trying to load credentials from various sources...
        GoogleCredentials credentials = loadCredentials();
        if (credentials != null) {
            builder.setCredentials(credentials);
            log.info("BigQuery credentials loaded successfully!");
        } else {
            log.warn("BigQuery credentials not loaded! Using default application credentials.");
        }

        return builder.build().getService();
    }

    private GoogleCredentials loadCredentials() {
        // First, try to load from the specified location
        if (credentialsLocation != null && !credentialsLocation.trim().isEmpty()) {
            try {
                Resource resource = resourceLoader.getResource(credentialsLocation);
                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        log.info("Loading credentials from: {}", credentialsLocation);
                        return GoogleCredentials.fromStream(inputStream);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load credentials from {}: {}", credentialsLocation, e.getMessage());
            }
        }

        // Fallback to environment variable GOOGLE_APPLICATION_CREDENTIALS
        String googleCredentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (googleCredentialsPath != null) {
            try {
                Resource resource = resourceLoader.getResource("file:" + googleCredentialsPath);
                if (resource.exists()) {
                    try (InputStream inputStream = resource.getInputStream()) {
                        log.info("Loading credentials from GOOGLE_APPLICATION_CREDENTIALS: {}",
                                googleCredentialsPath);
                        return GoogleCredentials.fromStream(inputStream);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to load credentials from GOOGLE_APPLICATION_CREDENTIALS {}: {}",
                        googleCredentialsPath, e.getMessage());
            }
        }

        // Try to use application default credentials
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            log.info("Using application default credentials");
            return credentials;
        } catch (Exception e) {
            log.warn("Failed to load application default credentials: {}", e.getMessage());
        }

        return null;
    }
}
