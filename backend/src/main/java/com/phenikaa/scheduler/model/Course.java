package com.phenikaa.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_code", length = 20, unique = true, nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "credits", nullable = false)
    private Double credits;

    @Column(name = "theory_credits", nullable = false)
    private Double theoryCredits;

    @Column(name = "practice_credits", nullable = false)
    private Double practiceCredits;

    // --- CÁC TRƯỜNG MỚI CẦN BỔ SUNG ĐỂ TỰ ĐỘNG TÁCH LỚP ---

    // Sĩ số tối đa cho lớp Lý thuyết (VD: 100 hoặc 120 cho giảng đường lớn)
    @Column(name = "theory_quota_limit")
    private Integer theoryQuotaLimit = 60; // Mặc định 60 nếu không set

    // Sĩ số tối đa cho lớp Thực hành (VD: 40 cho phòng Lab)
    @Column(name = "practice_quota_limit")
    private Integer practiceQuotaLimit = 40; // Mặc định 40

    // Đánh dấu môn học Online/E-learning (Để gom lớp siêu to)
    @Column(name = "is_online")
    private Boolean isOnline = false;

    // --------------------------------------------------------

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = true)
    private Faculty faculty;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = true)
    private School school;

    private Boolean isCompulsory = true;

    @ManyToOne
    @JoinColumn(name = "managing_faculty_id", nullable = true)
    private Faculty managingFaculty;

    @ManyToMany(mappedBy = "teachingCourses", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("teachingCourses")
    private List<Lecturer> lecturers;
}