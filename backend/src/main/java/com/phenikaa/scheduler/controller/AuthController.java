package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.User;
import com.phenikaa.scheduler.repository.UserRepository;
import com.phenikaa.scheduler.security.jwt.JwtUtils;
import lombok.AllArgsConstructor; 
import lombok.Data;
import lombok.NoArgsConstructor;
// --------------------------------
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder encoder;
    @Autowired JwtUtils jwtUtils;

    // API Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        // 1. Tìm user trong DB
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. Kiểm tra mật khẩu
        if (!encoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Error: Incorrect password");
        }

        // 3. Tạo Token
        String jwt = jwtUtils.generateJwtToken(user.getUsername());

        // 4. Trả về thông tin cho Frontend
        // Lỗi constructor sẽ hết sau khi import AllArgsConstructor bên dưới
        return ResponseEntity.ok(new JwtResponse(
                jwt, 
                user.getId(), 
                user.getUsername(), 
                user.getRole(),
                user.getFaculty() != null ? user.getFaculty().getId() : null,
                user.getFullName()
        ));
    }

    @PostMapping("/setup")
    public String setupUsers() {
        if (userRepository.count() > 0) return "Users already exist";
        
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(encoder.encode("123456"));
        admin.setRole("ADMIN_TRUONG");
        admin.setFullName("Super Admin");
        userRepository.save(admin);
        
        return "Setup done. Login with admin/123456";
    }
}

// --- DTO Classes (Data Transfer Objects) ---

@Data
@NoArgsConstructor
@AllArgsConstructor 
class LoginRequest { 
    private String username; 
    private String password; 
}

@Data 
@NoArgsConstructor // Nên thêm cái này để tránh lỗi Jackson deserialize
@AllArgsConstructor // <-- Cái này quan trọng để sửa lỗi "constructor undefined"
class JwtResponse { 
    private String token; 
    private Long id; 
    private String username; 
    private String role;
    private Long facultyId;
    private String fullName;
}