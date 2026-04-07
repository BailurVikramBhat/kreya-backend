package com.kreya.auth.service;

import com.kreya.auth.dto.RegisterRequest;
import com.kreya.auth.entity.Credential;
import com.kreya.auth.repository.CredentialRepository;
import com.kreya.user.entity.Role;
import com.kreya.user.entity.User;
import com.kreya.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {
    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CredentialRepository credentialRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

    }
}
