package com.bitiriciler32.cms.management.repository;

import com.bitiriciler32.cms.management.entity.CameraEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Long> {

    /** Returns only non-deleted cameras. Used everywhere an admin/operator sees the camera list. */
    List<CameraEntity> findAllByDeletedFalse();

    /** Looks up a camera by id only if it has not been soft-deleted. */
    Optional<CameraEntity> findByIdAndDeletedFalse(Long id);

    /** Check existence, ignoring soft-deleted rows. */
    boolean existsByIdAndDeletedFalse(Long id);
}
