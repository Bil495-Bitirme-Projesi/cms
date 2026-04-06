package com.bitiriciler32.cms.notification.repository;

import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.notification.entity.DeviceFcmTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeviceFcmTokenRepository extends JpaRepository<DeviceFcmTokenEntity, Long> {

    List<DeviceFcmTokenEntity> findByUserAndEnabledTrue(UserEntity user);

    void deleteByUser(UserEntity user);
}
