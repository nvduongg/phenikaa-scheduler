package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name; // Ví dụ: A2-301, C4-502

    @Column(nullable = false)
    private Integer capacity; // Sức chứa: 60, 40, 120...

    @Column(nullable = false)
    private String type; // THEORY (Lý thuyết), PC (Phòng máy tính), LAB (Thực hành/Thí nghiệm), HALL (Hội trường)
}