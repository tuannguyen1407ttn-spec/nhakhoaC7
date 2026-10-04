package com.c7smile.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "dentists")
public class Dentist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(nullable = false)
    private String cccd;

    private LocalDate dob;

    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String bio; // Can be used as introduction

    @Column(unique = true)
    private String slug;

    private String position;
    
    @Column(columnDefinition = "TEXT")
    private String shortDescription;
    
    @Column(columnDefinition = "TEXT")
    private String training;
    
    @Column(columnDefinition = "TEXT")
    private String experience;
    
    @Column(columnDefinition = "TEXT")
    private String certificates;
    
    @Column(columnDefinition = "TEXT")
    private String achievements;
    
    @Column(columnDefinition = "TEXT")
    private String expertise;
    private String status = "Hoạt động";

    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (this.slug == null || this.slug.isEmpty()) {
            if (this.fullName != null) {
                this.slug = this.fullName.toLowerCase()
                    .replaceAll("[áàảãạăắằẳẵặâấầẩẫậ]", "a")
                    .replaceAll("[éèẻẽẹêếềểễệ]", "e")
                    .replaceAll("[íìỉĩị]", "i")
                    .replaceAll("[óòỏõọôốồổỗộơớờởỡợ]", "o")
                    .replaceAll("[úùủũụưứừửữự]", "u")
                    .replaceAll("[ýỳỷỹỵ]", "y")
                    .replaceAll("đ", "d")
                    .replaceAll("[^a-z0-9]", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "") + "-" + System.currentTimeMillis();
            }
        }
    }
}
