package com.company.bank_system.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Table(name = "email_verification_tokens")
@Entity
@Getter
@Setter
public class EmailConfirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String mailKeyHash;
    private int attempts;

    private LocalDateTime created_at;
    private LocalDateTime expires_at;
    private boolean used;

}
