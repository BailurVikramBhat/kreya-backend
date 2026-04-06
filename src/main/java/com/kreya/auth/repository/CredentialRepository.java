package com.kreya.auth.repository;

import com.kreya.auth.entity.Credential;
import com.kreya.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByEmail(String email);
    Optional<Credential> findByUser(User user);
}
