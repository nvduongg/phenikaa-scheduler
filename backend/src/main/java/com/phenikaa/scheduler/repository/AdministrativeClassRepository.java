package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.AdministrativeClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdministrativeClassRepository extends JpaRepository<AdministrativeClass, Long> {
    Optional<AdministrativeClass> findByName(String name);
    List<AdministrativeClass> findByMajorFacultySchoolId(Long schoolId);
}