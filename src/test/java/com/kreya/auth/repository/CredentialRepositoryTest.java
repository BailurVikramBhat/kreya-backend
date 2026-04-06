package com.kreya.auth.repository;

import com.kreya.auth.entity.Credential;
import com.kreya.user.entity.Role;
import com.kreya.user.entity.User;
import com.kreya.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class CredentialRepositoryTest {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindByEmail() {
        User user = createAndSaveUser("find-by-email-user@kreya.com");
        Credential credential = createCredential(user, "find-by-email-cred@kreya.com");
        credentialRepository.saveAndFlush(credential);

        Optional<Credential> found = credentialRepository.findByEmail("find-by-email-cred@kreya.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find-by-email-cred@kreya.com");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void shouldSaveAndFindByUser() {
        User user = createAndSaveUser("find-by-user-user@kreya.com");
        Credential credential = createCredential(user, "find-by-user-cred@kreya.com");
        credentialRepository.saveAndFlush(credential);

        Optional<Credential> found = credentialRepository.findByUser(user);

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void whenDuplicateEmailThenFail() {
        User user1 = createAndSaveUser("dup-email-user1@kreya.com");
        User user2 = createAndSaveUser("dup-email-user2@kreya.com");

        Credential first = createCredential(user1, "dup-email-cred@kreya.com");
        credentialRepository.saveAndFlush(first);

        Credential second = createCredential(user2, "dup-email-cred@kreya.com");

        assertThatThrownBy(() -> credentialRepository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenSecondCredentialForSameUserThenFail() {
        User user = createAndSaveUser("dup-user-user@kreya.com");

        Credential first = createCredential(user, "dup-user-cred1@kreya.com");
        credentialRepository.saveAndFlush(first);

        Credential second = createCredential(user, "dup-user-cred2@kreya.com");

        assertThatThrownBy(() -> credentialRepository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User createAndSaveUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setRole(Role.SHOPPER);
        user.setVikram(false);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.saveAndFlush(user);
    }

    private Credential createCredential(User user, String email) {
        Credential credential = new Credential();
        credential.setUser(user);
        credential.setEmail(email);
        credential.setPasswordHash("$2a$10$hashedpassword");
        credential.setEmailVerified(false);
        credential.setMfaEnabled(false);
        credential.setAccountLocked(false);
        credential.setFailedAttempts(0);
        credential.setCreatedAt(LocalDateTime.now());
        credential.setUpdatedAt(LocalDateTime.now());
        return credential;
    }
}
