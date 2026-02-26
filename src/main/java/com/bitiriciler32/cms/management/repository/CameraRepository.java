package com.bitiriciler32.cms.management.repository;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Long> {
}
