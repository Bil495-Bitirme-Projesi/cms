package com.bitiriciler32.cms.anomaly.service;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves which users should receive an alert for a given camera.
 * Uses the camera-to-user access mapping defined by the Management module.
 */
@Component
@RequiredArgsConstructor
public class RecipientResolver {

    private final UserCameraAccessRepository userCameraAccessRepository;

    /**
     * Returns all users who have access to the given camera.
     */
    public List<UserEntity> resolveRecipients(CameraEntity camera, String type) {
        List<UserCameraAccessEntity> accessList = userCameraAccessRepository.findByCamera(camera);
        return accessList.stream()
                .map(UserCameraAccessEntity::getUser)
                .collect(Collectors.toList());
    }
}
