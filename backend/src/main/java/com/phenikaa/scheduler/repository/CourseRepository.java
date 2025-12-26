package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    // Tìm môn học theo mã (để map từ Excel)
    Optional<Course> findByCourseCode(String courseCode);
}