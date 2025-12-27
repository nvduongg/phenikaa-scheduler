package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Lưu hash (BCrypt)

    // Role: "ADMIN_TRUONG" (Super Admin), "ADMIN_KHOA" (Faculty Admin), "GIANG_VIEN"
    @Column(nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;   // Dùng cho ADMIN_SCHOOL (Mới thêm)

    // QUAN TRỌNG: User này thuộc khoa nào?
    // Nếu là ADMIN_TRUONG -> null (quản lý tất)
    // Nếu là ADMIN_KHOA -> trỏ về khoa cụ thể (VD: Khoa CNTT)
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty; 
    
    private String fullName;
}