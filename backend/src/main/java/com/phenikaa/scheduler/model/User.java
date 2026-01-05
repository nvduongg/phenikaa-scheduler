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

    // Role: "ADMIN" (Đại học), "ADMIN_SCHOOL" (Trường), "ADMIN_FACULTY" (Khoa)
    @Column(nullable = false)
    private String role;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;   // Dùng cho ADMIN_SCHOOL (Mới thêm)

    // QUAN TRỌNG: User này thuộc khoa nào?
    // Nếu là ADMIN -> null (quản lý tất)
    // Nếu là ADMIN_FACULTY -> trỏ về khoa cụ thể (VD: Khoa CNTT)
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty; 
    
    private String fullName;
}