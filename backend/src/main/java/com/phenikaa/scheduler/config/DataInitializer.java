package com.phenikaa.scheduler.config;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.model.User;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.SchoolRepository;
import com.phenikaa.scheduler.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private FacultyRepository facultyRepo;
    @Autowired private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println(">>> STARTING DATA INITIALIZATION (PHENIKAA HIERARCHY)...");

        // 1. KHỞI TẠO CÁC TRƯỜNG THÀNH VIÊN (MEMBER SCHOOLS)
        School psc = createSchool("PSC", "Phenikaa School of Computing");
        School pseb = createSchool("PSEB", "Phenikaa School of Economics and Business");
        School pfs = createSchool("PFS", "Phenikaa School of Social Sciences and Humanities");
        School psmp = createSchool("PSMP", "Phenikaa School of Medicine and Pharmacy");
        School pse = createSchool("PSE", "Phenikaa School of Engineering");

        // 2. KHỞI TẠO CÁC KHOA (FACULTIES) VÀ GÁN VÀO TRƯỜNG (SCHOOL)

        // --- Group A: Thuộc PSC (Trường Máy tính) ---
        createFaculty("PSC1", "Faculty of Computer Science", psc);
        createFaculty("PSC2", "Faculty of Information Systems", psc);
        createFaculty("PSC3", "Faculty of Artificial Intelligence", psc);

        // --- Group B: Thuộc PSEB (Trường Kinh tế & Kinh doanh) ---
        createFaculty("FBA", "Faculty of Business Administration", pseb);
        createFaculty("EIB", "Faculty of Economics and International Business", pseb);
        createFaculty("FFA", "Faculty of Finance and Accounting", pseb);
        createFaculty("FTH", "Faculty of Tourism and Hospitality", pseb);

        // --- Group C: Thuộc PFS (Trường KHXH & NV) ---
        createFaculty("FL", "Faculty of English Language", pfs);
        createFaculty("FFL", "Faculty of French Language", pfs);
        createFaculty("FCL", "Faculty of Chinese Language", pfs);
        createFaculty("FKL", "Faculty of Korean Language", pfs);
        createFaculty("FJL", "Faculty of Japanese Language", pfs);
        createFaculty("FOS", "Faculty of Oriental Studies", pfs);

        // --- Group D: Thuộc PSMP (Trường Y Dược) ---
        createFaculty("MD", "Faculty of Medicine", psmp);
        createFaculty("PUD", "Faculty of Odonto-Stomatology", psmp); // Răng Hàm Mặt
        createFaculty("FTME", "Faculty of Traditional Medicine", psmp);
        createFaculty("FP", "Faculty of Pharmacy", psmp);
        createFaculty("FN", "Faculty of Nursing", psmp);
        createFaculty("ME", "Faculty of Medical Engineering", psmp); // Kỹ thuật Y học
        createFaculty("FPH", "Faculty of Public Health", psmp);
        createFaculty("BMS", "Faculty of Biomedical Science", psmp);

        // --- Group E: Thuộc PSE (Trường Kỹ thuật) ---
        createFaculty("BCEE", "Faculty of Biotechnology, Chemistry and Environmental Engineering", pse);
        createFaculty("EEE", "Faculty of Electrical and Electronics Engineering", pse);
        createFaculty("MEM", "Faculty of Mechanical and Mechatronics Engineering", pse);
        createFaculty("MSE", "Faculty of Materials Science and Engineering", pse);
        createFaculty("VEE", "Faculty of Automotive and Energy Engineering", pse);

        // --- Group F: KHOA ĐỘC LẬP (Trực thuộc Đại học - KHÔNG thuộc School nào) ---
        createFaculty("FOL", "Faculty of Law", null);
        createFaculty("FIDT", "Faculty of Interdisciplinary Digital Technology", null);
        createFaculty("FS", "Faculty of Fundamental Sciences", null);

        // 3. KHỞI TẠO USER MẪU (ADMIN)
        initUsers();

        System.out.println(">>> DATA INITIALIZATION COMPLETED.");
    }

    // --- HELPER METHODS ---

    private School createSchool(String code, String name) {
        return schoolRepo.findByCode(code).orElseGet(() -> {
            School s = new School();
            s.setCode(code);
            s.setName(name);
            System.out.println("Created School: " + code);
            return schoolRepo.save(s);
        });
    }

    private Faculty createFaculty(String code, String name, School school) {
        // Tìm theo Code để chính xác nhất
        return facultyRepo.findByCode(code).orElseGet(() -> {
            Faculty f = new Faculty();
            f.setCode(code);
            f.setName(name);
            f.setSchool(school); // Link Faculty -> School (hoặc null)
            System.out.println("Created Faculty: " + code + (school != null ? " -> " + school.getCode() : " (Independent)"));
            return facultyRepo.save(f);
        });
    }

    private void initUsers() {
        // --- CẤP 1: ADMIN ĐẠI HỌC (CAO NHẤT) ---
        // Không thuộc trường nào, khoa nào -> Quản lý tất cả
        createUser("admin", "123456", "ADMIN", "Phenikaa Admin", null, null);

        // --- CẤP 2: ADMIN TRƯỜNG THÀNH VIÊN (ADMIN_SCHOOL) ---
        // Ví dụ: Giáo vụ Trường Máy tính (PSC) -> Quản lý PSC1, PSC2, PSC3...
        School psc = schoolRepo.findByCode("PSC").orElse(null);
        if (psc != null) {
            createUser("admin_psc", "123456", "ADMIN_SCHOOL", "Trường Công nghệ Thông tin Phenikaa", null, psc);
        }

        School pseb = schoolRepo.findByCode("PSEB").orElse(null);
        if (pseb != null) {
            createUser("admin_pseb", "123456", "ADMIN_SCHOOL", "Trường Kinh tế Phenikaa", null, pseb);
        }

        // --- CẤP 3: ADMIN KHOA CHUYÊN MÔN (ADMIN_FACULTY) ---
        
        // Ví dụ: Giáo vụ Khoa KH Máy tính (PSC1 - Con của PSC)
        Faculty csFaculty = facultyRepo.findByCode("PSC1").orElse(null);
        if (csFaculty != null) {
            createUser("staff_cs", "123456", "ADMIN_FACULTY", "Giáo vụ Khoa KHMT", csFaculty, null);
            createUser("staff_is", "123456", "ADMIN_FACULTY", "Giáo vụ Khoa HTTT", csFaculty, null);
        }

        // Ví dụ: Giáo vụ Khoa Quản trị kinh doanh (FBA - Con của PSEB)
        Faculty fbaFaculty = facultyRepo.findByCode("FBA").orElse(null);
        if (fbaFaculty != null) {
            createUser("staff_fba", "123456", "ADMIN_FACULTY", "Giáo vụ Khoa QTKD", fbaFaculty, null);
        }
        
        // Ví dụ: Giáo vụ Khoa KH Cơ bản (FS - Độc lập)
        Faculty fsFaculty = facultyRepo.findByCode("FS").orElse(null);
        if (fsFaculty != null) {
            // Khoa độc lập thì quyền tương đương Admin Khoa, nhưng báo cáo trực tiếp lên Đại học
            createUser("staff_fs", "123456", "ADMIN_FACULTY", "Giáo vụ Khoa KHCB", fsFaculty, null);
        }
    }

    private void createUser(String username, String pass, String role, String fullName, Faculty f, School s) {
        if (!userRepository.existsByUsername(username)) {
            User u = new User();
            u.setUsername(username);
            u.setPassword(passwordEncoder.encode(pass));
            u.setRole(role); // ADMIN, ADMIN_SCHOOL, hoặc ADMIN_FACULTY
            u.setFullName(fullName);
            u.setFaculty(f);
            u.setSchool(s);
            userRepository.save(u);
            System.out.println(">> Created User: " + username + " [" + role + "]");
        }
    }
}