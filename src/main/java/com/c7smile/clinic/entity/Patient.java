package com.c7smile.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    private String phone;
    
    private String email;

    private LocalDate dob;

    private String gender; // Nam, Nữ, Khác

    private String cccd;

    private String address;

    // Optional: Liên kết với tài khoản đăng nhập quản lý bệnh nhân này
    // Nếu account = null => Khách vãng lai (walk-in) chưa đăng ký tài khoản online
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private User account;
    
    // Ghi chú về tiền sử bệnh lý (dị ứng, bệnh nền, v.v.)
    @Column(columnDefinition = "TEXT")
    private String medicalHistoryNotes;
}
