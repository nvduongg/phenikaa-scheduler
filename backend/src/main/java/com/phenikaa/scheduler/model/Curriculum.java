package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Entity
@Table(name = "curricula")
@Data
public class Curriculum {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên CTĐT: "CTĐT CNTT K17 - 2023"
    @Column(nullable = false)
    private String name;

    // CTĐT này dành cho Ngành nào?
    @ManyToOne
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    // Dành cho Khóa nào?
    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    // Danh sách các môn trong CTĐT này
    // (Thường sẽ có thêm bảng phụ để lưu học kỳ dự kiến, nhưng ở đây mapping đơn giản trước)
    @ManyToMany
    @JoinTable(
        name = "curriculum_courses",
        joinColumns = @JoinColumn(name = "curriculum_id"),
        inverseJoinColumns = @JoinColumn(name = "course_code")
    )
    private List<Course> courses;
}