package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "schools")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên Trường: "Trường Khoa học Máy tính", "PHX Smart School"
    @Column(nullable = false, unique = true)
    private String name;

    // Mã Trường: "SCS", "PHX"
    @Column(unique = true, length = 20)
    private String code;

    // Quan hệ 1-N: Một Trường có nhiều Khoa trực thuộc
    // Ví dụ: Trường KHMT có Khoa Kỹ thuật phần mềm, Khoa AI...
    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Faculty> faculties;
    
    // (Mở rộng) Hiệu trưởng trường thành viên, Văn phòng trường...
}