package com.phenikaa.scheduler.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "administrative_classes")
@Data
public class AdministrativeClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên lớp biên chế: "K17 CNTT 01", "K19 Logistic 02"
    // Dữ liệu từ cột "Nhóm KS" trong file CSV
    @Column(nullable = false, unique = true)
    private String name;

    // Mã lớp (nếu có quy định riêng): "K17-7480201-01"
    @Column(unique = true)
    private String code;

    // Lớp này thuộc Ngành nào?
    @ManyToOne
    @JoinColumn(name = "major_id", nullable = false)
    private Major major;

    // Thuộc Khóa nào? (K17, K18...)
    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    // Sĩ số lớp biên chế (Để ước lượng khi xếp phòng cho môn bắt buộc)
    private Integer size;
}