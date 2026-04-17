package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.DuplicateResourceException;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.StreamStatus;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.management.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-043 (UT): AccessControlService.grantAccess()   [note: IT test is TC-CMS-043]
 * TC-CMS-044 (UT): AccessControlService.revokeAccess()
 * TC-CMS-045 (UT): AccessControlService.getAccessList()
 *
 * These are the UT counterparts of the IT test cases TC-CMS-043/044/045.
 * See deviations doc for numbering details.
 */
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @Mock UserRepository userRepository;
    @Mock CameraRepository cameraRepository;
    @Mock UserCameraAccessRepository userCameraAccessRepository;

    @InjectMocks AccessControlService accessControlService;

    private UserEntity operator(Long id) {
        return UserEntity.builder().id(id).name("Op").email("op@t.com")
                .passwordHash("h").role(Role.OPERATOR).enabled(true).tokenVersion(1L).build();
    }

    private CameraEntity camera(Long id) {
        return CameraEntity.builder().id(id).name("Cam").rtspUrl("rtsp://x")
                .detectionEnabled(true).streamStatus(StreamStatus.UNKNOWN).deleted(false).build();
    }

    // ── grantAccess() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("grantAccess()")
    class GrantAccessTests {

        @Test
        @DisplayName("(1) valid user and camera, no existing access - grants access")
        void grantAccess_valid_grants() {
            UserEntity user = operator(1L);
            CameraEntity cam = camera(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(userCameraAccessRepository.existsByUserAndCamera(user, cam)).thenReturn(false);

            accessControlService.grantAccess(1L, 1L);

            verify(userCameraAccessRepository).save(any(UserCameraAccessEntity.class));
        }

        @Test
        @DisplayName("(2) user not found - throws ResourceNotFoundException")
        void grantAccess_userNotFound_throwsResourceNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.grantAccess(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(3) camera not found - throws ResourceNotFoundException")
        void grantAccess_cameraNotFound_throwsResourceNotFoundException() {
            UserEntity user = operator(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.grantAccess(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(4) access already exists - throws DuplicateResourceException")
        void grantAccess_alreadyExists_throwsDuplicateResourceException() {
            UserEntity user = operator(1L);
            CameraEntity cam = camera(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(userCameraAccessRepository.existsByUserAndCamera(user, cam)).thenReturn(true);

            assertThatThrownBy(() -> accessControlService.grantAccess(1L, 1L))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("(5) UserCameraAccessRepository.save() throws - propagates")
        void grantAccess_repositoryThrows_propagates() {
            UserEntity user = operator(1L);
            CameraEntity cam = camera(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(userCameraAccessRepository.existsByUserAndCamera(user, cam)).thenReturn(false);
            when(userCameraAccessRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> accessControlService.grantAccess(1L, 1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── revokeAccess() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("revokeAccess()")
    class RevokeAccessTests {

        @Test
        @DisplayName("(1) valid user, camera, existing access - revokes")
        void revokeAccess_valid_revokes() {
            UserEntity user = operator(1L);
            CameraEntity cam = camera(1L);
            UserCameraAccessEntity access = UserCameraAccessEntity.builder()
                    .id(1L).user(user).camera(cam).build();
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(userCameraAccessRepository.findByUserAndCamera(user, cam))
                    .thenReturn(Optional.of(access));

            accessControlService.revokeAccess(1L, 1L);

            verify(userCameraAccessRepository).delete(access);
        }

        @Test
        @DisplayName("(2) user not found - throws ResourceNotFoundException")
        void revokeAccess_userNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.revokeAccess(99L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(3) camera not found - throws ResourceNotFoundException")
        void revokeAccess_cameraNotFound_throws() {
            UserEntity user = operator(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.revokeAccess(1L, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(4) access mapping not found - throws ResourceNotFoundException")
        void revokeAccess_accessNotFound_throws() {
            UserEntity user = operator(1L);
            CameraEntity cam = camera(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(cameraRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(cam));
            when(userCameraAccessRepository.findByUserAndCamera(user, cam))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.revokeAccess(1L, 1L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── getAccessList() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAccessList()")
    class GetAccessListTests {

        @Test
        @DisplayName("(1) user exists with multiple access entries - returns list")
        void getAccessList_multipleEntries_returnsList() {
            UserEntity user = operator(1L);
            CameraEntity cam1 = camera(1L);
            CameraEntity cam2 = camera(2L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userCameraAccessRepository.findByUser(user)).thenReturn(List.of(
                    UserCameraAccessEntity.builder().id(1L).user(user).camera(cam1).build(),
                    UserCameraAccessEntity.builder().id(2L).user(user).camera(cam2).build()
            ));

            var result = accessControlService.getAccessList(1L);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("(2) user exists with no access entries - returns empty list")
        void getAccessList_noEntries_returnsEmpty() {
            UserEntity user = operator(1L);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(userCameraAccessRepository.findByUser(user)).thenReturn(List.of());

            var result = accessControlService.getAccessList(1L);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("(3) user not found - throws ResourceNotFoundException")
        void getAccessList_userNotFound_throws() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accessControlService.getAccessList(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

