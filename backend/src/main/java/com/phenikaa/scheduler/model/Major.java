package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "majors")
@Data
public class Major {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // "Công nghệ thông tin", "Dược học"

    @Column(unique = true, nullable = false)
    private String code; // "7480201", "CNT"

    // Ngành thuộc Khoa nào?
    @ManyToOne
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;
}