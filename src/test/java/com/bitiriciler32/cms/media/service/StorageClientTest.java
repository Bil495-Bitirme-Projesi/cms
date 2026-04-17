package com.bitiriciler32.cms.media.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-031: StorageClient.presignPut()
 * TC-CMS-032: StorageClient.presignGet()
 */
@ExtendWith(MockitoExtension.class)
class StorageClientTest {

    @Mock MinioClient minioClient;

    StorageClient storageClient;

    @BeforeEach
    void setUp() {
        storageClient = new StorageClient(minioClient);
        ReflectionTestUtils.setField(storageClient, "bucketName", "test-bucket");
    }

    // ── TC-CMS-031: presignPut() ─────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-031: StorageClient.presignPut()")
    class PresignPutTests {

        @Test
        @DisplayName("(1) valid objectKey and expirySeconds - returns presigned PUT URL")
        void presignPut_valid_returnsPutUrl() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://minio/put-url");

            String result = storageClient.presignPut("cameras/1/events/1.mp4", 300L);

            assertThat(result).isEqualTo("http://minio/put-url");
        }

        @Test
        @DisplayName("(2) MinioClient throws exception - wraps in RuntimeException")
        void presignPut_minioClientThrows_wrapsInRuntimeException() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new RuntimeException("MinIO error"));

            assertThatThrownBy(() -> storageClient.presignPut("key", 300L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(5) null objectKey - builder throws IllegalArgumentException (wrapped in RuntimeException)")
        void presignPut_nullObjectKey_throwsRuntimeException() {
            assertThatThrownBy(() -> storageClient.presignPut(null, 300L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-032: presignGet() ─────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-032: StorageClient.presignGet()")
    class PresignGetTests {

        @Test
        @DisplayName("(1) valid objectKey and expirySeconds - returns presigned GET URL")
        void presignGet_valid_returnsGetUrl() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn("http://minio/get-url");

            String result = storageClient.presignGet("cameras/1/events/1.mp4", 300L);

            assertThat(result).isEqualTo("http://minio/get-url");
        }

        @Test
        @DisplayName("(2) MinioClient throws exception - wraps in RuntimeException")
        void presignGet_minioClientThrows_wrapsInRuntimeException() throws Exception {
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenThrow(new RuntimeException("MinIO error"));

            assertThatThrownBy(() -> storageClient.presignGet("key", 300L))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(5) null objectKey - builder throws (wrapped in RuntimeException)")
        void presignGet_nullObjectKey_throwsRuntimeException() {
            assertThatThrownBy(() -> storageClient.presignGet(null, 300L))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

