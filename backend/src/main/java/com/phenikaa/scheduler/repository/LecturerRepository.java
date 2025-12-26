package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByLecturerCode(String lecturerCode);
}