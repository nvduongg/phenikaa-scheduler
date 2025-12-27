package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Entity
@Table(name = "curriculum_details")
@Data
public class CurriculumDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curriculum_id", nullable = false)
    // Bỏ qua các trường rác của Hibernate Proxy khi chuyển sang JSON
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "curriculumDetails", "courses"}) 
    private Curriculum curriculum;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_code", nullable = false)
    // Bỏ qua các trường rác của Hibernate Proxy
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "curriculumDetails"}) 
    private Course course;

    @Column(name = "semester_index", nullable = false)
    private String semesterIndex; // Thay đổi từ Integer sang String để lưu "1,2" hoặc "1"
    
    // Tổng số tín chỉ của môn này trong CTĐT này (có thể khác nhau giữa các ngành)
    private Integer credits; 
}