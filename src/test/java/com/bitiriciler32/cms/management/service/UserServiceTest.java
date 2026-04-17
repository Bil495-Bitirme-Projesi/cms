package com.bitiriciler32.cms.management.service;

import com.bitiriciler32.cms.common.exception.DuplicateResourceException;
import com.bitiriciler32.cms.common.exception.ResourceNotFoundException;
import com.bitiriciler32.cms.management.dto.CreateUserRequest;
import com.bitiriciler32.cms.management.dto.UpdateUserRequest;
import com.bitiriciler32.cms.management.dto.UserResponse;
import com.bitiriciler32.cms.management.entity.Role;
import com.bitiriciler32.cms.management.entity.UserEntity;
import com.bitiriciler32.cms.management.repository.UserCameraAccessRepository;
import com.bitiriciler32.cms.management.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * TC-CMS-036: UserService.create()
 * TC-CMS-037: UserService.update()
 * TC-CMS-038: UserService.delete()
 * TC-CMS-039: UserService.findById()
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock UserCameraAccessRepository userCameraAccessRepository;

    @InjectMocks UserService userService;

    private UserEntity savedUser(Long id, String name, String email, Role role, boolean enabled) {
        return UserEntity.builder()
                .id(id).name(name).email(email)
                .passwordHash("hashed").role(role).enabled(enabled).tokenVersion(1L).build();
    }

    // ── TC-CMS-036: create() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-036: UserService.create()")
    class CreateTests {

        @Test
        @DisplayName("(1) valid request with enabled=true - creates user")
        void create_enabledTrue_succeeds() {
            CreateUserRequest req = new CreateUserRequest("Alice", "alice@t.com", "pass", Role.OPERATOR, true);
            UserEntity saved = savedUser(1L, "Alice", "alice@t.com", Role.OPERATOR, true);
            when(userRepository.existsByEmail("alice@t.com")).thenReturn(false);
            when(passwordEncoder.encode("pass")).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(saved);

            UserResponse result = userService.create(req);

            assertThat(result.getEmail()).isEqualTo("alice@t.com");
            assertThat(result.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("(2) valid request with enabled=false - creates user")
        void create_enabledFalse_succeeds() {
            CreateUserRequest req = new CreateUserRequest("Bob", "bob@t.com", "pass", Role.OPERATOR, false);
            UserEntity saved = savedUser(2L, "Bob", "bob@t.com", Role.OPERATOR, false);
            when(userRepository.existsByEmail("bob@t.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(saved);

            UserResponse result = userService.create(req);

            assertThat(result.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("(3) null enabled - defaults to true")
        void create_nullEnabled_defaultsToTrue() {
            CreateUserRequest req = new CreateUserRequest("Carol", "carol@t.com", "pass", Role.OPERATOR, null);
            UserEntity saved = savedUser(3L, "Carol", "carol@t.com", Role.OPERATOR, true);
            when(userRepository.existsByEmail("carol@t.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenReturn(saved);

            UserResponse result = userService.create(req);

            assertThat(result.getEnabled()).isTrue();
        }

        @Test
        @DisplayName("(4) email already exists - throws DuplicateResourceException")
        void create_duplicateEmail_throwsDuplicateResourceException() {
            CreateUserRequest req = new CreateUserRequest("Dave", "dave@t.com", "pass", Role.OPERATOR, true);
            when(userRepository.existsByEmail("dave@t.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.create(req))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("(5) PasswordEncoder.encode() throws - propagates")
        void create_encoderThrows_propagates() {
            CreateUserRequest req = new CreateUserRequest("Eve", "eve@t.com", "pass", Role.OPERATOR, true);
            when(userRepository.existsByEmail("eve@t.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenThrow(new RuntimeException("encode error"));

            assertThatThrownBy(() -> userService.create(req))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("(6) UserRepository.save() throws - propagates")
        void create_repositoryThrows_propagates() {
            CreateUserRequest req = new CreateUserRequest("Frank", "frank@t.com", "pass", Role.OPERATOR, true);
            when(userRepository.existsByEmail("frank@t.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> userService.create(req))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-037: update() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-037: UserService.update()")
    class UpdateTests {

        @Test
        @DisplayName("(1) user exists, update name only - updates name")
        void update_nameOnly_updatesName() {
            UserEntity existing = savedUser(1L, "Old", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenReturn(existing);

            UpdateUserRequest req = new UpdateUserRequest("New", null, null);
            UserResponse result = userService.update(1L, req);

            assertThat(existing.getName()).isEqualTo("New");
        }

        @Test
        @DisplayName("(2) user exists, update enabled only - updates enabled")
        void update_enabledOnly_updatesEnabled() {
            UserEntity existing = savedUser(1L, "U", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenReturn(existing);

            UpdateUserRequest req = new UpdateUserRequest(null, false, null);
            userService.update(1L, req);

            assertThat(existing.getEnabled()).isFalse();
        }

        @Test
        @DisplayName("(3) user exists, update role only (OPERATOR→ADMIN) - revokes camera access, increments tokenVersion")
        void update_roleToAdmin_revokesAccessAndIncrementsToken() {
            UserEntity existing = savedUser(1L, "U", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenReturn(existing);

            UpdateUserRequest req = new UpdateUserRequest(null, null, Role.ADMIN);
            userService.update(1L, req);

            assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
            assertThat(existing.getTokenVersion()).isEqualTo(2L); // incremented
            verify(userCameraAccessRepository).deleteAllByUser(existing);
        }

        @Test
        @DisplayName("(4) user exists, update all fields - updates all")
        void update_allFields_updatesAll() {
            UserEntity existing = savedUser(1L, "Old", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenReturn(existing);

            UpdateUserRequest req = new UpdateUserRequest("New", false, Role.ADMIN);
            userService.update(1L, req);

            assertThat(existing.getName()).isEqualTo("New");
            assertThat(existing.getEnabled()).isFalse();
            assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        @DisplayName("(5) user exists, all fields null - no changes")
        void update_nullFields_noChanges() {
            UserEntity existing = savedUser(1L, "U", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenReturn(existing);

            UpdateUserRequest req = new UpdateUserRequest(null, null, null);
            userService.update(1L, req);

            assertThat(existing.getName()).isEqualTo("U");
            assertThat(existing.getRole()).isEqualTo(Role.OPERATOR);
        }

        @Test
        @DisplayName("(6) user not found - throws ResourceNotFoundException")
        void update_notFound_throwsResourceNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.update(99L, new UpdateUserRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("(7) UserRepository.save() throws - propagates")
        void update_repositoryThrows_propagates() {
            UserEntity existing = savedUser(1L, "U", "u@t.com", Role.OPERATOR, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(any())).thenThrow(new RuntimeException("DB error"));

            assertThatThrownBy(() -> userService.update(1L, new UpdateUserRequest("New", null, null)))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ── TC-CMS-038: delete() ─────────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-038: UserService.delete()")
    class DeleteTests {

        @Test
        @DisplayName("(1) user exists - deletes successfully")
        void delete_exists_deletes() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.delete(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("(2) user not found - throws ResourceNotFoundException")
        void delete_notFound_throwsResourceNotFoundException() {
            when(userRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> userService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── TC-CMS-039: findById() ───────────────────────────────────────────────

    @Nested
    @DisplayName("TC-CMS-039: UserService.findById()")
    class FindByIdTests {

        @Test
        @DisplayName("(1) user exists - returns UserResponse")
        void findById_exists_returnsUserResponse() {
            UserEntity user = savedUser(1L, "Alice", "alice@t.com", Role.ADMIN, true);
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserResponse result = userService.findById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("alice@t.com");
        }

        @Test
        @DisplayName("(2) user not found - throws ResourceNotFoundException")
        void findById_notFound_throwsResourceNotFoundException() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}

