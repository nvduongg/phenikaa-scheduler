package com.phenikaa.scheduler.config;

import com.phenikaa.scheduler.model.User;
import com.phenikaa.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> STARTING DATA INITIALIZATION (ADMIN ONLY)...");
        createAdminIfNotExists();
        System.out.println(">>> DATA INITIALIZATION COMPLETED.");
    }

    private void createAdminIfNotExists() {
        String username = "admin";
        if (!userRepository.existsByUsername(username)) {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode("123456"));
            u.setRole("ADMIN");
            u.setFullName("Phenikaa Admin");
            // Nếu entity User có quan hệ tới Faculty/School, set null để bỏ liên kết mặc định
            try {
                u.getClass().getMethod("setFaculty", Class.forName("com.phenikaa.scheduler.model.Faculty")).invoke(u, new Object[]{null});
            } catch (Exception ignored) {}
            try {
                u.getClass().getMethod("setSchool", Class.forName("com.phenikaa.scheduler.model.School")).invoke(u, new Object[]{null});
            } catch (Exception ignored) {}
            userRepository.save(u);
            System.out.println(">> Created User: " + username + " [ADMIN]");
        }
    }
}