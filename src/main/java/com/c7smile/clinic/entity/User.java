package com.c7smile.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // Email dùng để đăng nhập

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "CUSTOMER"; // ADMIN, RECEPTION, DOCTOR, CASHIER, CUSTOMER

    private String phone;

    private LocalDate dob;

    @Column(columnDefinition = "boolean default true")
    private boolean enabled = true; // Mặc định true cho các user hệ thống/đã có

    private String verificationToken;

    private LocalDateTime tokenExpiry;
}
