package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    // Tìm lớp theo mã (để import)
    java.util.Optional<CourseOffering> findByCode(String code);
    
    // Đếm số lớp giảng viên đang dạy (để cân bằng tải)
    long countByLecturerId(Long lecturerId);
}