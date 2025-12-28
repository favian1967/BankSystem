package com.company.bank_system.entity;


import com.company.bank_system.entity.enums.User.UserRole;
import com.company.bank_system.entity.enums.User.UserStatus;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = {"passwordHash", "accounts"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Account> accounts;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false,  unique = true)
    public String phone;

    @Column(nullable = false)
    private String passwordHash;

    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate birthDate;

    @Column(unique = true)
    private String passportNumber;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserStatus status; // ACTIVE, BLOCKED
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

}
