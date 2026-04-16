package com.company.bank_system.repo;

import com.company.bank_system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findUserById(Long id);
}

