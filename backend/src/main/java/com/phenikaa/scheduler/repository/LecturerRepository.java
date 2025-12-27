package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    Optional<Lecturer> findByLecturerCode(String lecturerCode);

    // Tìm giảng viên thuộc 1 khoa
    List<Lecturer> findByFacultyId(Long facultyId);

    // Tìm các giảng viên thuộc về 1 Trường thành viên
    // Logic: Lecturer -> Faculty -> School
    List<Lecturer> findByFaculty_School_Id(Long schoolId);
}