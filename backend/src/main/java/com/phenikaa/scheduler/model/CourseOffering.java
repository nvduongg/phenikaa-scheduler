package com.phenikaa.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_offerings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Mã lớp học phần (Ví dụ: 20242-CSE702011-01)

    // --- SỬA TẠI ĐÂY ---
    // Cách 1: Liên kết theo ID (Khuyên dùng) - Đổi tên cột FK thành course_id cho chuẩn
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false) 
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Course course;

    // HOẶC Cách 2: Nếu bạn bắt buộc muốn giữ liên kết theo Mã môn (course_code)
    // @ManyToOne
    // @JoinColumn(name = "course_code", referencedColumnName = "course_code", nullable = false) // Sửa courseCode thành course_code
    // @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    // private Course course;
    // -------------------

    @Column(name = "planned_size")
    private Integer plannedSize;

    @Column(name = "target_classes")
    private String targetClasses;

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "teachingCourses"})
    private Lecturer lecturer;

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Room room; 

    private Integer dayOfWeek; 
    private Integer startPeriod;
    private Integer endPeriod;
    
    @Column(nullable = false)
    private String status = "PLANNED"; 

    @Column(name = "status_message")
    private String statusMessage;
}