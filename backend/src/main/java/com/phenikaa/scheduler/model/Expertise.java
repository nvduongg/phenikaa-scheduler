package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "expertise", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"lecturer_id", "course_code"}) // 1 GV - 1 Môn chỉ xuất hiện 1 lần
})
@Data
public class Expertise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecturer_id", nullable = false)
    private Lecturer lecturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_code", nullable = false)
    private Course course;

    // PRIMARY (Dạy chính/Sở trường), SECONDARY (Dạy thay/Có thể dạy)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkillLevel level; 

    public enum SkillLevel {
        PRIMARY,    // Ưu tiên xếp lịch cho môn này
        SECONDARY,  // Chỉ xếp khi thiếu người
        LAB_ONLY    // Chỉ dạy thực hành
    }
}