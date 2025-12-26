package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {
    Optional<Curriculum> findByName(String name);
}