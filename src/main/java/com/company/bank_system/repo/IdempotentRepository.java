package com.company.bank_system.repo;

import com.company.bank_system.entity.IdempotentEntity;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotentRepository extends JpaRepository<IdempotentEntity, Long> {
    @Modifying
    @Query(value = """
  update idempotent
  set status = 'SUCCESS', transaction_id = :txId
  where account_id = :accountId and idempotency_key = :key
""", nativeQuery = true)
    int markSuccess(@Param("accountId") Long accountId,
                    @Param("key") String key,
                    @Param("txId") Long txId);
    @Modifying
    @Query(value = """
  insert into idempotent(account_id, idempotency_key, status)
  values (:accountId, :key, 'IN_PROGRESS')
  on conflict (account_id, idempotency_key) do nothing
""", nativeQuery = true)
    int tryInsert(@Param("accountId") Long accountId, @Param("key") String key);


    Optional<IdempotentEntity> findByAccountIdAndIdempotencyKey(Long accountId, String key);
}