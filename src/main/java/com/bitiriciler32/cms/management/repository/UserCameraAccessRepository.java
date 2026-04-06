package com.bitiriciler32.cms.management.repository;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import com.bitiriciler32.cms.management.entity.UserCameraAccessEntity;
import com.bitiriciler32.cms.management.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCameraAccessRepository extends JpaRepository<UserCameraAccessEntity, Long> {

    List<UserCameraAccessEntity> findByCamera(CameraEntity camera);

    List<UserCameraAccessEntity> findByUser(UserEntity user);

    Optional<UserCameraAccessEntity> findByUserAndCamera(UserEntity user, CameraEntity camera);

    boolean existsByUserAndCamera(UserEntity user, CameraEntity camera);

    void deleteByUserAndCamera(UserEntity user, CameraEntity camera);

    void deleteAllByUser(UserEntity user);

    void deleteAllByCamera(CameraEntity camera);
}
