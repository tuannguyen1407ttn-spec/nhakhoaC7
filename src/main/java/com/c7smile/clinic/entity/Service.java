package com.c7smile.clinic.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "services")
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    
    private Double price;

    private String imageUrl;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;

    @Column(columnDefinition = "TEXT")
    private String detailedDescription;

    private String duration;
    
    @Column(columnDefinition = "TEXT")
    private String process;
    
    @Column(columnDefinition = "TEXT")
    private String benefits;
    
    @Column(columnDefinition = "TEXT")
    private String suitableFor;
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(columnDefinition = "TEXT")
    private String aftercare;
    
    private String status = "Hoạt động";

    @PrePersist
    @PreUpdate
    private void generateSlug() {
        if (this.slug == null || this.slug.isEmpty()) {
            if (this.name != null) {
                this.slug = this.name.toLowerCase()
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
