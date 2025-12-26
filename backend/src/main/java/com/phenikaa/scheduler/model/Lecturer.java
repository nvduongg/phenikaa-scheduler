package com.phenikaa.scheduler.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "lecturers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã nhân sự: "PU0390" - Quan trọng để định danh duy nhất
    @Column(name = "lecturer_code", nullable = false, unique = true)
    private String lecturerCode;

    @Column(nullable = false)
    private String fullName; // "Nguyễn Văn A"

    @Column(unique = true)
    private String email; // Để gửi thông báo lịch dạy

    // Giảng viên thuộc biên chế Khoa nào?
    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    // (Mở rộng sau này) Chức danh: GS, PGS, TS, ThS... 
    // Có thể dùng để ưu tiên xếp lịch cho GS/PGS trước.
    private String title; 

    @ManyToMany(fetch = FetchType.EAGER) // Eager để load luôn danh sách môn khi get Lecturer
    @JoinTable(
        name = "lecturer_expertise",
        joinColumns = @JoinColumn(name = "lecturer_id"),
        inverseJoinColumns = @JoinColumn(name = "course_code") // Map với Course Code
    )
    // Chặn Jackson serialize ngược lại từ Course -> Lecturer để tránh vòng lặp
    @JsonIgnoreProperties("lecturers") 
    private List<Course> teachingCourses = new ArrayList<>();
}