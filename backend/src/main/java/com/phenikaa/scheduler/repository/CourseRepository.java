package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseCode(String courseCode);
    // Tìm nhiều môn cùng lúc theo danh sách mã (ví dụ: ["CSE101", "INT202"])
    List<Course> findByCourseCodeIn(List<String> courseCodes); 
}