package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Curriculum;
import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.CurriculumDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CurriculumDetailRepository extends JpaRepository<CurriculumDetail, Long> {
    // Tìm chi tiết xem môn này đã có trong CTĐT chưa
    Optional<CurriculumDetail> findByCurriculumAndCourse(Curriculum curriculum, Course course);
}