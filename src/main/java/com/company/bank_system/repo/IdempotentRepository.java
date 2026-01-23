package com.company.bank_system.repo;

import com.company.bank_system.entity.IdempotentEntity;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface IdempotentRepository extends JpaRepository<IdempotentEntity, Long> {
    Optional<IdempotentEntity> findByAccountIdAndIdempotencyKey(Long accountId, String key);

    void deleteAllByCreatedAtBefore(LocalDateTime threshold);
}