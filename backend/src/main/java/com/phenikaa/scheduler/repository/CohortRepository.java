package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByName(String name);
}