package com.bitiriciler32.cms.security.repository;

import com.bitiriciler32.cms.security.entity.SubsystemCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubsystemCredentialRepository extends JpaRepository<SubsystemCredentialEntity, Long> {

    Optional<SubsystemCredentialEntity> findBySubsystemId(String subsystemId);
}
