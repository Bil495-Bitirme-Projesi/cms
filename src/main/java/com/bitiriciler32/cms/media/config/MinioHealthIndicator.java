package com.bitiriciler32.cms.media.config;

import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Actuator health indicator for MinIO object storage.
 *
 * Visible at GET /actuator/health under the "minio" component key.
 *
 * Checks connectivity by calling bucketExists() on the configured bucket.
 * This is a lightweight operation — it sends a HEAD request to MinIO,
 * no data is read or written.
 *
 * Status:
 *   UP   — MinIO is reachable and the bucket exists
 *   DOWN — MinIO is unreachable or the bucket is missing
 */
@Component("minio")
@RequiredArgsConstructor
@Slf4j
public class MinioHealthIndicator implements HealthIndicator {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public Health health() {
        try {
            boolean exists = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (exists) {
                return Health.up()
                        .withDetail("endpoint", endpoint)
                        .withDetail("bucket", bucketName)
                        .build();
            } else {
                return Health.down()
                        .withDetail("endpoint", endpoint)
                        .withDetail("bucket", bucketName)
                        .withDetail("reason", "Bucket '" + bucketName + "' does not exist")
                        .build();
            }
        } catch (MinioException e) {
            log.warn("MinIO health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("endpoint", endpoint)
                    .withDetail("reason", e.getMessage())
                    .build();
        } catch (Exception e) {
            log.warn("MinIO health check failed unexpectedly: {}", e.getMessage());
            return Health.down()
                    .withDetail("endpoint", endpoint)
                    .withDetail("reason", e.getMessage())
                    .build();
        }
    }
}

