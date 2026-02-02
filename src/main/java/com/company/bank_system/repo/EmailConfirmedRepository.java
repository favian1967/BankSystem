package com.company.bank_system.repo;

import com.company.bank_system.entity.EmailConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


public interface EmailConfirmedRepository extends JpaRepository<EmailConfirmation, Long> {

    Optional<EmailConfirmation> findByUserId(Long userId);
}
