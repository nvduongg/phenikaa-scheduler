package com.phenikaa.scheduler.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_offerings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Mã lớp học phần (Ví dụ: 20242-CSE702011-01)

    // Liên kết với Môn học
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "lecturers"}) // Tránh load lại danh sách GV của môn
    private Course course;

    // --- CÁC TRƯỜNG MỚI ĐỂ HỖ TRỢ TÁCH LỚP (STRUCTURE-BASED) ---

    // 1. Loại lớp: "LT" (Lý thuyết), "TH" (Thực hành), "ELN" (Online), "ALL" (Lớp thường)
    @Column(name = "class_type")
    private String classType = "ALL"; 

    // 1b. Yêu cầu loại phòng (nếu muốn ép phòng theo nhu cầu riêng)
    // VD: "LAB" (phòng PC/phòng máy), "THEORY", "HALL", "ONLINE"
    // Nếu null/blank thì hệ thống sẽ suy luận dựa trên classType và đặc tính môn.
    @Column(name = "required_room_type")
    private String requiredRoomType;

    // 2. Mối quan hệ Cha - Con (Lớp TH trỏ về Lớp LT của nó)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "parent", "children"}) // Chặn vòng lặp vô hạn JSON
    private CourseOffering parent;

    // 3. Liên kết với Kỳ học (Để biết lớp này thuộc kỳ nào)
    @ManyToOne
    @JoinColumn(name = "semester_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Semester semester;

    // -----------------------------------------------------------

    @Column(name = "planned_size")
    private Integer plannedSize;

    @Column(name = "target_classes")
    private String targetClasses; // K17-CNTT1; K17-CNTT2

    // --- KẾT QUẢ XẾP LỊCH (OUTPUT) ---

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "teachingCourses"})
    private Lecturer lecturer;

    @ManyToOne
    @JoinColumn(name = "room_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Room room; 

    private Integer dayOfWeek; 
    private Integer startPeriod;
    private Integer endPeriod;
    
    @Column(nullable = false)
    private String status = "PLANNED"; // PLANNED, SCHEDULED, ERROR

    @Column(name = "status_message")
    private String statusMessage; // Ghi chú lỗi nếu xếp lịch thất bại
}