package com.phenikaa.scheduler.repository;

import com.phenikaa.scheduler.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Tìm kiếm user bằng username (Dùng trong UserDetailsServiceImpl để login)
    Optional<User> findByUsername(String username);

    // Kiểm tra xem username đã tồn tại chưa (Dùng khi tạo user mới để tránh trùng)
    Boolean existsByUsername(String username);
}