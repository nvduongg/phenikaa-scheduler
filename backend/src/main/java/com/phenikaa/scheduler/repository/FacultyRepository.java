package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Faculty;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FacultyRepository extends JpaRepository<Faculty, String> {
    // Tìm khoa theo mã
    Optional<Faculty> findByCode(String code);
}