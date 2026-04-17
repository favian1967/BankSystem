package com.company.bank_system.repo;

import com.company.bank_system.entity.EmailConfirmation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface EmailConfirmedRepository extends JpaRepository<EmailConfirmation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailConfirmation> findByUserId(Long userId);
}