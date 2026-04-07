package com.kreya.auth.service;

import com.kreya.auth.dto.RegisterRequest;
import com.kreya.auth.entity.Credential;
import com.kreya.auth.repository.CredentialRepository;
import com.kreya.user.entity.Role;
import com.kreya.user.entity.User;
import com.kreya.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-h2.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CredentialRepository credentialRepository;

    @BeforeEach
    void setup() {
        credentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void whenRegisterAsShopperThenRegistrationSuccessful() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("shopper@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Bhat");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(false);
        authService.register(request);

        Optional<User> savedUser = userRepository.findByEmail(request.getEmail());
        Optional<Credential> savedCredential = credentialRepository.findByEmail(request.getEmail());

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getRole()).isEqualTo(Role.SHOPPER);

        assertThat(savedCredential).isPresent();
        assertThat(savedCredential.get().getPasswordHash()).isNotEqualTo(request.getPassword());
        assertThat(savedCredential.get().isEmailVerified()).isFalse();
    }

    @Test
    void whenRegisterAsSellerThenRegistrationSuccessful() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("seller@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Bhat");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(true);
        authService.register(request);

        Optional<User> savedUser = userRepository.findByEmail(request.getEmail());
        Optional<Credential> savedCredential = credentialRepository.findByEmail(request.getEmail());

        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getRole()).isEqualTo(Role.SELLER);
        assertThat(savedCredential).isPresent();
    }

    @Test
    void whenRegisterWithExistingEmailThenThrowIllegalArgumentException() {
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("user1@kreya.com");
        request1.setPassword("Password123!");
        request1.setFirstName("Vikram");
        request1.setLastName("Bhat");
        request1.setPhone("9999999999");
        request1.setRegisteringAsSeller(false);

        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("user1@kreya.com");
        request2.setPassword("Password123!");
        request2.setFirstName("Vikram");
        request2.setLastName("Bhat");
        request2.setPhone("9999999999");
        request2.setRegisteringAsSeller(false);

        authService.register(request1);
        assertThatThrownBy(() -> authService.register(request2)).isInstanceOf(IllegalArgumentException.class).hasMessage("Email already registered");
    }

    @Test
    void shouldStoreEncodedPassword() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("password-check@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Singh Rathore");
        request.setPhone("9990001111");
        request.setRegisteringAsSeller(false);

        authService.register(request);

        Optional<Credential> savedCredential = credentialRepository.findByEmail("password-check@kreya.com");

        assertThat(savedCredential).isPresent();
        assertThat(savedCredential.get().getPasswordHash()).isNotEqualTo("Password123!");
        assertThat(savedCredential.get().getPasswordHash()).isNotBlank();
    }

}