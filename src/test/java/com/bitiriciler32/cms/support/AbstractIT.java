package com.bitiriciler32.cms.support;

import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for integration tests.
 *
 * <ul>
 *   <li>Spins up a PostgreSQL Testcontainer (shared across all IT subclasses via static field).</li>
 *   <li>Mocks the MinioClient bean so that MinIO is never contacted during tests.</li>
 *   <li>Firebase initialisation is silently skipped because the credentials file does not exist
 *       in the test environment (FirebaseConfig already handles this gracefully).</li>
 *   <li>Provides a {@link TestTokenFactory} for generating JWT tokens for ADMIN, OPERATOR,
 *       and SUBSYSTEM principals.</li>
 *   <li>Creates fresh ADMIN and OPERATOR users before each test via {@link #seedUsers()}.</li>
 * </ul>
 *
 * <h3>Usage</h3>
 * <pre>
 * class MyCameraIT extends AbstractIT {
 *
 *     {@literal @}Test
 *     void createCamera_asAdmin_returns201() throws Exception {
 *         mockMvc.perform(post("/api/cameras")
 *                 .header("Authorization", "Bearer " + adminToken())
 *                 .contentType(APPLICATION_JSON)
 *                 .content("{...}"))
 *             .andExpect(status().isCreated());
 *     }
 * }
 * </pre>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIT {


    // ── MinIO — mocked to avoid real object-storage calls in IT tests ──

    /**
     * Replaces the real MinioClient bean with a Mockito mock.
     * Tests that need specific MinIO behaviour should stub methods on this mock.
     */
    @MockitoBean
    protected MinioClient minioClient;

    // ── MVC & helpers ──

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestTokenFactory tokenFactory;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    // ── Seeded users (available after @BeforeEach) ──

    protected UserEntity adminUser;
    protected UserEntity operatorUser;

    /**
     * Seeds a fresh ADMIN and OPERATOR user before every test.
     * All previous data in the tables is wiped first so tests are fully isolated.
     */
    @BeforeEach
    void seedUsers() {
        userRepository.deleteAll();

        adminUser = userRepository.save(UserEntity.builder()
                .name("Test Admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("admin-pass"))
                .role(Role.ADMIN)
                .enabled(true)
                .tokenVersion(1L)
                .build());

        operatorUser = userRepository.save(UserEntity.builder()
                .name("Test Operator")
                .email("operator@test.com")
                .passwordHash(passwordEncoder.encode("operator-pass"))
                .role(Role.OPERATOR)
                .enabled(true)
                .tokenVersion(1L)
                .build());
    }

    // ── Token shortcuts ──

    /** Returns a valid Bearer token for the seeded ADMIN user. */
    protected String adminToken() {
        return tokenFactory.forUser(adminUser);
    }

    /** Returns a valid Bearer token for the seeded OPERATOR user. */
    protected String operatorToken() {
        return tokenFactory.forUser(operatorUser);
    }

    /** Returns a valid subsystem Bearer token. */
    protected String subsystemToken() {
        return tokenFactory.forSubsystem();
    }
}



