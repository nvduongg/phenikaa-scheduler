package com.phenikaa.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @Column(name = "course_code", length = 20) // Mã HP làm khóa chính, VD: "CSE702011"
    private String courseCode;

    @Column(nullable = false)
    private String name; // Tên HP: "Điện toán đám mây"

    // Tổng số tín chỉ (VD: 3)
    @Column(name = "credits", nullable = false)
    private Integer credits;

    // Số tín chỉ Lý thuyết (VD: 2) -> Dùng để tính số tiết LT
    @Column(name = "theory_credits", nullable = false)
    private Integer theoryCredits;

    // Số tín chỉ Thực hành (VD: 1) -> Dùng để tính số tiết TH
    @Column(name = "practice_credits", nullable = false)
    private Integer practiceCredits;

    // Môn học thuộc Khoa nào? (Ràng buộc FK)
    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    // Tính chất môn: Bắt buộc, Tự chọn (Optional)
    // Giúp xếp lịch ưu tiên các môn bắt buộc trước
    private Boolean isCompulsory = true; 

    // Khoa chịu trách nhiệm chuyên môn
    @ManyToOne
    @JoinColumn(name = "managing_faculty_id", nullable = false)
    private Faculty managingFaculty;

    // Giang viên có chuyên môn về môn này
    @ManyToMany(mappedBy = "teachingCourses", fetch = FetchType.EAGER) // Eager để load luôn danh sách giảng viên khi get Course
    @JsonIgnoreProperties("teachingCourses") // Chặn Jackson serialize ngược lại từ Lecturer -> Course để tránh vòng lặp
    private java.util.List<Lecturer> lecturers;
}