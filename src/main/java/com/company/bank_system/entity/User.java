package com.company.bank_system.entity;


import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@ToString(exclude = "passwordHash")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    private String status;
    @Column(updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
