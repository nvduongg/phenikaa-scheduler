package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
    // Tìm lớp theo mã (để import)
    java.util.Optional<CourseOffering> findByCode(String code);
    
    // Đếm số lớp giảng viên đang dạy (để cân bằng tải)
    long countByLecturerId(Long lecturerId);

    // Tìm các lớp thuộc chuyên môn của khoa này (Để Khoa KHCB vào gán GV)
    List<CourseOffering> findByCourse_ManagingFaculty_Id(Long facultyId);

    // Tìm các lớp học phần thuộc về 1 Trường thành viên (thông qua Managing Faculty HOẶC trực tiếp School)
    @Query("SELECT c FROM CourseOffering c " +
           "LEFT JOIN c.course co " +
           "LEFT JOIN co.managingFaculty mf " +
           "LEFT JOIN mf.school mfs " +
           "LEFT JOIN co.school cos " +
           "WHERE mfs.id = :schoolId OR cos.id = :schoolId")
    List<CourseOffering> findAllBySchoolId(@Param("schoolId") Long schoolId);

    // Lấy danh sách lớp theo Kỳ học
    List<CourseOffering> findBySemester_Id(Long semesterId);
}