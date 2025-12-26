package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "teaching_assignments")
@Data
public class TeachingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Phân công GIẢNG VIÊN nào?
    @ManyToOne
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    // Phân công vào LỚP HỌC PHẦN nào?
    @ManyToOne
    @JoinColumn(name = "offering_id", nullable = false)
    private CourseOffering courseOffering;

    // Vai trò giảng dạy (Quan trọng cho tính thù lao/giờ chuẩn)
    @Enumerated(EnumType.STRING)
    private AssignmentRole role;

    // Trạng thái phân công
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;

    public enum AssignmentRole {
        MAIN_LECTURER,      // Giảng viên chính (Lên lớp LT)
        LAB_INSTRUCTOR,     // Hướng dẫn thực hành
        TEACHING_ASSISTANT, // Trợ giảng
        GUEST_SPEAKER       // Thỉnh giảng
    }

    public enum AssignmentStatus {
        DRAFT,      // Khoa đề xuất, chưa chốt
        CONFIRMED,  // Đã chốt, hiển thị lên TKB
        REJECTED    // Giảng viên từ chối hoặc bận
    }
}