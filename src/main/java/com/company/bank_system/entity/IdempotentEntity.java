package com.company.bank_system.entity;


import com.company.bank_system.entity.enums.Idempotent.IdempotentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity

@Table(
        name = "idempotent",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_idem_account_key",
                        columnNames = {"account_id", "idempotency_key"}
                )
        }
)

@Getter
@Setter
public class IdempotentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private IdempotentStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Account account;

    private Long transactionId;

    private LocalDateTime createdAt;
}
