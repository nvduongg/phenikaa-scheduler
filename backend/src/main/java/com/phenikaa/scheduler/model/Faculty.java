package com.phenikaa.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "faculties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên Khoa: "Khoa Kỹ thuật phần mềm", "Khoa Khoa học cơ bản"
    @Column(nullable = false, unique = true)
    private String name;

    @Column(unique = true, length = 20)
    private String code;

    // CẬP NHẬT: Khoa thuộc Trường thành viên nào?
    // Có thể null nếu là Khoa trực thuộc Đại học
    @ManyToOne
    @JoinColumn(name = "school_id", nullable = true) 
    private School school;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL)
    @JsonIgnore 
    private List<Course> courses;

    @OneToMany(mappedBy = "faculty", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Lecturer> lecturers;
}