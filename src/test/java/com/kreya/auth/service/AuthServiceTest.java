package com.kreya.auth.service;

import com.kreya.auth.dto.LoginRequest;
import com.kreya.auth.dto.RegisterRequest;
import com.kreya.auth.dto.TokenResponse;
import com.kreya.auth.dto.VerifyEmailRequest;
import com.kreya.auth.entity.Credential;
import com.kreya.auth.entity.EmailVerificationToken;
import com.kreya.auth.repository.CredentialRepository;
import com.kreya.auth.repository.EmailVerificationTokenRepository;
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
    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @BeforeEach
    void setup() {
        emailVerificationTokenRepository.deleteAll();
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
        assertThat(emailVerificationTokenRepository.findByUser(savedUser.get())).isPresent();
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

    @Test
    void whenLoginWithValidCredentialsThenReturnTokenResponse() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("login-user@kreya.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("Vikram");
        registerRequest.setLastName("Bhat");
        registerRequest.setPhone("9999999999");
        registerRequest.setRegisteringAsSeller(false);

        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("login-user@kreya.com");
        loginRequest.setPassword("Password123!");

        TokenResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void whenLoginWithUnknownEmailThenThrowIllegalArgumentException() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unknown@kreya.com");
        loginRequest.setPassword("Password123!");

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid email or password");
    }

    @Test
    void whenLoginWithWrongPasswordThenThrowIllegalArgumentException() {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("wrong-password@kreya.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setFirstName("Vikram");
        registerRequest.setLastName("Bhat");
        registerRequest.setPhone("9999999999");
        registerRequest.setRegisteringAsSeller(false);

        authService.register(registerRequest);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("wrong-password@kreya.com");
        loginRequest.setPassword("WrongPassword123!");

        assertThatThrownBy(() -> authService.login(loginRequest))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid email or password");
    }

    @Test
    void whenVerifyEmailWithValidTokenThenEmailMarkedVerified() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("verify@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Bhat");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(false);

        authService.register(request);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        EmailVerificationToken token = emailVerificationTokenRepository.findByUser(user).orElseThrow();

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token.getToken());

        authService.verifyEmail(verifyRequest);

        Credential credential = credentialRepository.findByEmail(request.getEmail()).orElseThrow();
        EmailVerificationToken updatedToken = emailVerificationTokenRepository.findByToken(token.getToken()).orElseThrow();

        assertThat(credential.isEmailVerified()).isTrue();
        assertThat(updatedToken.getVerifiedAt()).isNotNull();
    }

    @Test
    void whenVerifyEmailWithInvalidTokenThenThrowIllegalArgumentException() {
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken("invalid-token");

        assertThatThrownBy(() -> authService.verifyEmail(verifyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid verification token");
    }

    @Test
    void whenVerifyEmailWithExpiredTokenThenThrowIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("expired@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Bhat");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(false);

        authService.register(request);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        EmailVerificationToken token = emailVerificationTokenRepository.findByUser(user).orElseThrow();
        token.setExpiresAt(token.getCreatedAt().minusMinutes(1));
        emailVerificationTokenRepository.save(token);

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token.getToken());

        assertThatThrownBy(() -> authService.verifyEmail(verifyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification token expired");
    }

    @Test
    void whenVerifyEmailWithUsedTokenThenThrowIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("used@kreya.com");
        request.setPassword("Password123!");
        request.setFirstName("Vikram");
        request.setLastName("Bhat");
        request.setPhone("9999999999");
        request.setRegisteringAsSeller(false);

        authService.register(request);

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        EmailVerificationToken token = emailVerificationTokenRepository.findByUser(user).orElseThrow();
        token.setVerifiedAt(token.getCreatedAt().plusMinutes(5));
        emailVerificationTokenRepository.save(token);

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token.getToken());

        assertThatThrownBy(() -> authService.verifyEmail(verifyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Verification token already used");
    }

}
