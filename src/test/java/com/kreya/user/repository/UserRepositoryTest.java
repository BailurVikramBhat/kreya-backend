package com.kreya.user.repository;

import com.kreya.user.entity.Role;
import com.kreya.user.entity.User;
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
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindByEmail() {
        User user = createUser("find-by-email-user@kreya.com", Role.SHOPPER);
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("find-by-email-user@kreya.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("find-by-email-user@kreya.com");
        assertThat(found.get().getFirstName()).isEqualTo("Alice");
        assertThat(found.get().getId()).isNotNull();
    }

    @Test
    void whenDuplicateEmailThenFail() {
        User first = createUser("dup-email-user@kreya.com", Role.SELLER);
        userRepository.saveAndFlush(first);

        User second = createUser("dup-email-user@kreya.com", Role.SHOPPER);

        assertThatThrownBy(() -> userRepository.saveAndFlush(second))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistRole() {
        for (Role role : Role.values()) {
            User user = createUser("persist-role-" + role.name().toLowerCase() + "@kreya.com", role);
            userRepository.saveAndFlush(user);

            Optional<User> found = userRepository.findByEmail(user.getEmail());
            assertThat(found).isPresent();
            assertThat(found.get().getRole()).isEqualTo(role);
        }
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setRole(role);
        user.setVikram(false);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }
}
