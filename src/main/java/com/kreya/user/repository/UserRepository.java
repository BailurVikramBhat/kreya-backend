package com.kreya.user.repository;

import com.kreya.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.modulith.NamedInterface;

import java.util.Optional;

@NamedInterface
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
