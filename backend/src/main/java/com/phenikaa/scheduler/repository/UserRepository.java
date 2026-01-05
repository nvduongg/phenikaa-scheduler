package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm user bằng username (Dùng trong UserDetailsServiceImpl để login)
    Optional<User> findByUsername(String username);

    // Kiểm tra xem username đã tồn tại chưa (Dùng khi tạo user mới để tránh trùng)
    Boolean existsByUsername(String username);

    // Dùng cho ADMIN_FACULTY: chỉ xem users thuộc khoa
    List<User> findByFacultyId(Long facultyId);

    // Dùng cho ADMIN_SCHOOL: thấy users thuộc trường mình (kể cả user thuộc khoa con)
    @Query("select u from User u left join u.school s left join u.faculty f left join f.school fs " +
            "where (s.id = :schoolId) or (fs.id = :schoolId)")
    List<User> findBySchoolScope(@Param("schoolId") Long schoolId);
}