package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Major;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MajorRepository extends JpaRepository<Major, Long> {
    Optional<Major> findByCode(String code);
    List<Major> findByFacultySchoolId(Long schoolId);
}