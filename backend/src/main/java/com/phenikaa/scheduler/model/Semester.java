package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "semesters")
@Data
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Ví dụ: "Học kỳ 1 2025-2026"

    @Column(name = "academic_year")
    private String academicYear; // "2025-2026"

    @Column(name = "term") // 1, 2, 3 (Kỳ hè)
    private Integer term;

    @Column(name = "is_current")
    private Boolean isCurrent = false; // Chỉ có 1 kỳ là true tại 1 thời điểm
}