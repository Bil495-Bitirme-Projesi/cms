package com.bitiriciler32.cms.config;

import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import com.bitiriciler32.cms.security.entity.SubsystemCredentialEntity;
import com.bitiriciler32.cms.security.repository.SubsystemCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with an initial admin user and AI subsystem credentials on first run.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final SubsystemCredentialRepository subsystemCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${subsystem.id}")
    private String subsystemId;

    @Value("${subsystem.secret}")
    private String subsystemSecret;

    @Override
    public void run(String... args) {
        // Create default admin if no users exist
        if (userRepository.count() == 0) {
            UserEntity admin = UserEntity.builder()
                    .name("System Administrator")
                    .email("admin@cms.local")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created: admin@cms.local / admin123");
        }

        // Create subsystem credentials if none exist
        if (subsystemCredentialRepository.count() == 0) {
            SubsystemCredentialEntity credential = SubsystemCredentialEntity.builder()
                    .subsystemId(subsystemId)
                    .subsystemSecret(subsystemSecret)
                    .scope("inference_sync")
                    .build();
            subsystemCredentialRepository.save(credential);
            log.info("Subsystem credential created: id={}", subsystemId);
        }
    }
}
