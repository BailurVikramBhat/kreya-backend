package com.kreya.auth.entity;

import com.kreya.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(schema = "auth_schema", name = "credentials")
@Getter
@Setter
@NoArgsConstructor
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "mfa_secret")
    private String mfaSecret;

    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts;

    @Column(name = "lock_expires_at")
    private LocalDateTime lockExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
