package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.School;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SchoolRepository extends JpaRepository<School, Long> {
    Optional<School> findByCode(String code);
}