package com.kreya.auth.service;

import com.kreya.auth.dto.LoginRequest;
import com.kreya.auth.dto.RegisterRequest;
import com.kreya.auth.dto.TokenResponse;
import com.kreya.auth.dto.VerifyEmailRequest;
import com.kreya.auth.entity.Credential;
import com.kreya.auth.entity.EmailVerificationToken;
import com.kreya.auth.repository.CredentialRepository;
import com.kreya.auth.repository.EmailVerificationTokenRepository;
import com.kreya.shared.security.JwtProvider;
import com.kreya.user.entity.Role;
import com.kreya.user.entity.User;
import com.kreya.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final CredentialRepository credentialRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(CredentialRepository credentialRepository,
                       EmailVerificationTokenRepository emailVerificationTokenRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.credentialRepository = credentialRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public void register(RegisterRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(u -> {
            throw new IllegalArgumentException("Email already registered");
        });
        credentialRepository.findByEmail(request.getEmail()).ifPresent(c -> {
            throw new IllegalArgumentException("Email already registered");
        });

        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(Boolean.TRUE.equals(request.getRegisteringAsSeller()) ? Role.SELLER : Role.SHOPPER);
        user.setActive(true);
        user.setVikram(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        Credential credential = new Credential();
        credential.setUser(savedUser);
        credential.setEmail(request.getEmail());
        credential.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        credential.setEmailVerified(false);
        credential.setMfaEnabled(false);
        credential.setAccountLocked(false);
        credential.setFailedAttempts(0);
        credential.setCreatedAt(now);
        credential.setUpdatedAt(now);

        credentialRepository.save(credential);

        EmailVerificationToken emailVerificationToken = new EmailVerificationToken();
        emailVerificationToken.setUser(savedUser);
        emailVerificationToken.setToken(UUID.randomUUID().toString());
        emailVerificationToken.setExpiresAt(now.plusHours(24));
        emailVerificationToken.setCreatedAt(now);

        emailVerificationTokenRepository.save(emailVerificationToken);

    }

    public TokenResponse login(LoginRequest request) {
        Credential cred = credentialRepository.findByEmail(request.getEmail()).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
        if(!passwordEncoder.matches(request.getPassword(), cred.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        User user = cred.getUser();
        String accessToken = jwtProvider.generateAccessToken(user);
        String refreshToken = jwtProvider.generateRefreshToken(user);
        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        return response;
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken emailVerificationToken = emailVerificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification token"));

        if (emailVerificationToken.getVerifiedAt() != null) {
            throw new IllegalArgumentException("Verification token already used");
        }

        if (emailVerificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Verification token expired");
        }

        Credential credential = credentialRepository.findByUser(emailVerificationToken.getUser())
                .orElseThrow(() -> new IllegalArgumentException("Credential not found"));

        credential.setEmailVerified(true);
        credential.setUpdatedAt(LocalDateTime.now());
        credentialRepository.save(credential);

        emailVerificationToken.setVerifiedAt(LocalDateTime.now());
        emailVerificationTokenRepository.save(emailVerificationToken);
    }
}
