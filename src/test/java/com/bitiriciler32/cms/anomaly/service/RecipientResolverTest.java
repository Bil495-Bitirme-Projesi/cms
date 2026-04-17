package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-016: RecipientResolver.resolveRecipients()
 */
@ExtendWith(MockitoExtension.class)
class RecipientResolverTest {

    @Mock UserCameraAccessRepository userCameraAccessRepository;

    @InjectMocks RecipientResolver recipientResolver;

    private CameraEntity camera() {
        return CameraEntity.builder().id(1L).name("Cam").rtspUrl("rtsp://x")
                .detectionEnabled(true).streamStatus(StreamStatus.UNKNOWN).deleted(false).build();
    }

    private UserEntity user(Long id, String email) {
        return UserEntity.builder().id(id).name("User").email(email)
                .passwordHash("hash").role(Role.OPERATOR).enabled(true).build();
    }

    private UserCameraAccessEntity access(UserEntity user, CameraEntity camera) {
        return UserCameraAccessEntity.builder().id(1L).user(user).camera(camera).build();
    }

    @Nested
    @DisplayName("TC-CMS-016: RecipientResolver.resolveRecipients()")
    class ResolveRecipientsTests {

        @Test
        @DisplayName("(1) camera has multiple users - returns all users")
        void resolveRecipients_multipleUsers_returnsAll() {
            CameraEntity cam = camera();
            UserEntity u1 = user(1L, "a@test.com");
            UserEntity u2 = user(2L, "b@test.com");
            when(userCameraAccessRepository.findByCamera(cam))
                    .thenReturn(List.of(access(u1, cam), access(u2, cam)));

            List<UserEntity> result = recipientResolver.resolveRecipients(cam, "INTRUSION");

            assertThat(result).containsExactlyInAnyOrder(u1, u2);
        }

        @Test
        @DisplayName("(2) camera has single user - returns single user")
        void resolveRecipients_singleUser_returnsSingleUser() {
            CameraEntity cam = camera();
            UserEntity u = user(1L, "a@test.com");
            when(userCameraAccessRepository.findByCamera(cam)).thenReturn(List.of(access(u, cam)));

            List<UserEntity> result = recipientResolver.resolveRecipients(cam, "INTRUSION");

            assertThat(result).containsExactly(u);
        }

        @Test
        @DisplayName("(3) camera has no users - returns empty list")
        void resolveRecipients_noUsers_returnsEmpty() {
            CameraEntity cam = camera();
            when(userCameraAccessRepository.findByCamera(cam)).thenReturn(List.of());

            List<UserEntity> result = recipientResolver.resolveRecipients(cam, "INTRUSION");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("(4) UserCameraAccessRepository throws exception - propagates")
        void resolveRecipients_repositoryThrows_propagates() {
            CameraEntity cam = camera();
            when(userCameraAccessRepository.findByCamera(cam))
                    .thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> recipientResolver.resolveRecipients(cam, "INTRUSION"))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}

