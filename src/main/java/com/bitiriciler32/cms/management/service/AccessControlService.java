package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.DuplicateResourceException;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.dto.UserCameraAccessResponse;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.CameraRepository;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final UserRepository userRepository;
    private final CameraRepository cameraRepository;
    private final UserCameraAccessRepository userCameraAccessRepository;

    @Transactional
    public void grantAccess(Long userId, Long cameraId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Camera access assignments do not apply to ADMIN users.");
        }

        CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", cameraId));

        if (userCameraAccessRepository.existsByUserAndCamera(user, camera)) {
            throw new DuplicateResourceException(
                    "User " + userId + " already has access to camera " + cameraId);
        }

        UserCameraAccessEntity access = UserCameraAccessEntity.builder()
                .user(user)
                .camera(camera)
                .build();

        userCameraAccessRepository.save(access);
    }

    @Transactional
    public void revokeAccess(Long userId, Long cameraId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        CameraEntity camera = cameraRepository.findByIdAndDeletedFalse(cameraId)
                .orElseThrow(() -> new ResourceNotFoundException("Camera", cameraId));

        UserCameraAccessEntity access = userCameraAccessRepository
                .findByUserAndCamera(user, camera)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Access mapping not found for user " + userId + " and camera " + cameraId));

        userCameraAccessRepository.delete(access);
    }

    @Transactional(readOnly = true)
    public List<UserCameraAccessResponse> getAccessList(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        return userCameraAccessRepository.findByUser(user).stream()
                .map(a -> new UserCameraAccessResponse(
                        a.getId(),
                        a.getUser().getId(),
                        a.getUser().getName(),
                        a.getUser().getEmail(),
                        a.getCamera().getId(),
                        a.getCamera().getName()
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}
