package com.bitiriciler32.cms.media.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Wrapper around MinioClient for generating presigned URLs.
 * Implements the StorageClient abstraction described in the LLD.
 */
@Component
@RequiredArgsConstructor
public class StorageClient {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    /** Generate a presigned PUT URL for uploading a clip. */
    public String presignPut(String objectKey, long expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry((int) expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned PUT URL", e);
        }
    }

    /** Generate a presigned GET URL for downloading/streaming a clip. */
    public String presignGet(String objectKey, long expirySeconds) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry((int) expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned GET URL", e);
        }
    }
}
