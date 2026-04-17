package com.bitiriciler32.cms.media.service;

import com.bitiriciler32.cms.media.dto.DownloadUrlResponse;
import com.bitiriciler32.cms.media.dto.UploadUrlResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-029: ClipStorageService.generateUploadUrl()
 * TC-CMS-030: ClipStorageService.generateDownloadUrl()
 */
@ExtendWith(MockitoExtension.class)
class ClipStorageServiceTest {

    @Mock StorageClient storageClient;

    ClipStorageService clipStorageService;

    @BeforeEach
    void setUp() {
        clipStorageService = new ClipStorageService(storageClient);
        ReflectionTestUtils.setField(clipStorageService, "presignExpirySeconds", 300L);
    }

    // ── TC-CMS-029: generateUploadUrl() ─────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-029: ClipStorageService.generateUploadUrl()")
    class GenerateUploadUrlTests {

        @Test
        @DisplayName("(1) valid cameraId and eventId - generates URL with correct key format")
        void generateUploadUrl_valid_returnsCorrectObjectKey() {
            when(storageClient.presignPut("cameras/5/events/10.mp4", 300L))
                    .thenReturn("http://minio/upload-url");

            UploadUrlResponse result = clipStorageService.generateUploadUrl(5L, 10L);

            assertThat(result.getObjectKey()).isEqualTo("cameras/5/events/10.mp4");
            assertThat(result.getUploadUrl()).isEqualTo("http://minio/upload-url");
            assertThat(result.getExpiresInSeconds()).isEqualTo(300L);
        }

        @Test
        @DisplayName("(2) StorageClient.presignPut() throws - propagates exception")
        void generateUploadUrl_storageClientThrows_propagates() {
            when(storageClient.presignPut(any(), anyLong()))
                    .thenThrow(new RuntimeException("MinIO error"));

            assertThatThrownBy(() -> clipStorageService.generateUploadUrl(1L, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("MinIO error");
        }

        @Test
        @DisplayName("(3) null cameraId - builds key with 'null' literal, delegates to StorageClient")
        void generateUploadUrl_nullCameraId_delegatesToStorageClient() {
            when(storageClient.presignPut("cameras/null/events/1.mp4", 300L))
                    .thenReturn("http://minio/url");

            UploadUrlResponse result = clipStorageService.generateUploadUrl(null, 1L);
            assertThat(result.getObjectKey()).isEqualTo("cameras/null/events/1.mp4");
        }

        @Test
        @DisplayName("(4) null eventId - builds key with 'null' literal, delegates to StorageClient")
        void generateUploadUrl_nullEventId_delegatesToStorageClient() {
            when(storageClient.presignPut("cameras/1/events/null.mp4", 300L))
                    .thenReturn("http://minio/url");

            UploadUrlResponse result = clipStorageService.generateUploadUrl(1L, null);
            assertThat(result.getObjectKey()).isEqualTo("cameras/1/events/null.mp4");
        }
    }

    // ── TC-CMS-030: generateDownloadUrl() ───────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-030: ClipStorageService.generateDownloadUrl()")
    class GenerateDownloadUrlTests {

        @Test
        @DisplayName("(1) valid objectKey - generates download URL")
        void generateDownloadUrl_valid_returnsUrl() {
            String key = "cameras/1/events/5.mp4";
            when(storageClient.presignGet(key, 300L)).thenReturn("http://minio/download-url");

            DownloadUrlResponse result = clipStorageService.generateDownloadUrl(key);

            assertThat(result.getDownloadUrl()).isEqualTo("http://minio/download-url");
            assertThat(result.getExpiresInSeconds()).isEqualTo(300L);
        }

        @Test
        @DisplayName("(2) StorageClient.presignGet() throws - propagates exception")
        void generateDownloadUrl_storageClientThrows_propagates() {
            when(storageClient.presignGet(any(), anyLong()))
                    .thenThrow(new RuntimeException("MinIO error"));

            assertThatThrownBy(() -> clipStorageService.generateDownloadUrl("any-key"))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(3) null objectKey - passes to StorageClient")
        void generateDownloadUrl_nullObjectKey_delegatesToStorageClient() {
            when(storageClient.presignGet(null, 300L)).thenReturn("http://url");

            assertThatCode(() -> clipStorageService.generateDownloadUrl(null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("(4) empty objectKey - passes to StorageClient")
        void generateDownloadUrl_emptyObjectKey_delegatesToStorageClient() {
            when(storageClient.presignGet("", 300L)).thenReturn("http://url");

            assertThatCode(() -> clipStorageService.generateDownloadUrl(""))
                    .doesNotThrowAnyException();
        }
    }
}

