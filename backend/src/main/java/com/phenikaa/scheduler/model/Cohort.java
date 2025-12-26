package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cohorts")
@Data
public class Cohort {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // "K17", "K18"

    private Integer startYear; // 2023
    private Integer endYear;   // 2027
}