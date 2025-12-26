package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    Optional<Semester> findByIsCurrentTrue();

    // Hàm để reset tất cả về false trước khi set kỳ mới làm current
    @Modifying
    @Transactional
    @Query("UPDATE Semester s SET s.isCurrent = false")
    void resetAllCurrent();
}