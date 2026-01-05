package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.model.User;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.SchoolRepository;
import com.phenikaa.scheduler.repository.UserRepository;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "http://localhost:5173")
public class UserManagementController {

    private final UserRepository userRepository;
    private final SchoolRepository schoolRepository;
    private final FacultyRepository facultyRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(
            UserRepository userRepository,
            SchoolRepository schoolRepository,
            FacultyRepository facultyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.schoolRepository = schoolRepository;
        this.facultyRepository = facultyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ResponseEntity<?> listUsers(Authentication authentication) {
        UserDetailsImpl principal = getPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean isAdmin = hasAuthority(principal, "ADMIN");
        boolean isAdminSchool = hasAuthority(principal, "ADMIN_SCHOOL");
        boolean isAdminFaculty = hasAuthority(principal, "ADMIN_FACULTY");

        // ADMIN_FACULTY không có quyền truy cập quản lý người dùng
        if (!isAdmin && !isAdminSchool) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        List<User> users;
        if (isAdmin) {
            users = userRepository.findAll();
        } else if (isAdminSchool) {
            Long schoolId = principal.getSchoolId();
            if (schoolId == null) return ResponseEntity.badRequest().body("Missing schoolId for ADMIN_SCHOOL");
            users = userRepository.findBySchoolScope(schoolId);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        List<UserSummaryDto> result = users.stream()
                .map(UserSummaryDto::from)
                .sorted(Comparator.comparing(UserSummaryDto::getUsername, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UpsertUserRequest request, Authentication authentication) {
        UserDetailsImpl principal = getPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Scope scope = scopeFor(principal);
        if (!scope.canManageUsers()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        String username = request != null ? trimToNull(request.getUsername()) : null;
        String password = request != null ? trimToNull(request.getPassword()) : null;
        String role = request != null ? trimToNull(request.getRole()) : null;
        String fullName = request != null ? trimToNull(request.getFullName()) : null;

        if (username == null) return ResponseEntity.badRequest().body("username is required");
        if (password == null) return ResponseEntity.badRequest().body("password is required");
        if (role == null) return ResponseEntity.badRequest().body("role is required");
        if (!isValidRole(role)) return ResponseEntity.badRequest().body("Invalid role: " + role);
        if (userRepository.existsByUsername(username)) return ResponseEntity.badRequest().body("Username already exists");

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);

        resolveAndApplyRoleScope(user, role, request, scope);

        userRepository.save(user);
        return ResponseEntity.ok(UserSummaryDto.from(user));
    }

    @PutMapping("/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<?> updateUser(@PathVariable long id, @RequestBody UpsertUserRequest request, Authentication authentication) {
        UserDetailsImpl principal = getPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Scope scope = scopeFor(principal);
        if (!scope.canManageUsers()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User target = opt.get();

        if (!scope.canAccess(target)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Out of scope");

        String role = request != null ? trimToNull(request.getRole()) : null;
        String fullName = request != null ? trimToNull(request.getFullName()) : null;
        String password = request != null ? trimToNull(request.getPassword()) : null;

        if (role != null) {
            // ADMIN/ADMIN_SCHOOL không được tự thay đổi quyền của chính mình
            if (target.getId() != null && target.getId().equals(principal.getId())
                    && (hasAuthority(principal, "ADMIN") || hasAuthority(principal, "ADMIN_SCHOOL"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot change your own role");
            }
            if (!isValidRole(role)) return ResponseEntity.badRequest().body("Invalid role: " + role);
            resolveAndApplyRoleScope(target, role, request, scope);
        }

        if (fullName != null) target.setFullName(fullName);
        if (password != null) target.setPassword(passwordEncoder.encode(password));

        userRepository.save(target);
        return ResponseEntity.ok(UserSummaryDto.from(target));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable long id, Authentication authentication) {
        UserDetailsImpl principal = getPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Scope scope = scopeFor(principal);
        if (!scope.canManageUsers()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        Optional<User> opt = userRepository.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        User target = opt.get();

        if (!scope.canAccess(target)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Out of scope");
        if (target.getId() != null && target.getId().equals(principal.getId())) {
            return ResponseEntity.badRequest().body("Cannot delete current user");
        }

        userRepository.delete(target);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importUsers(@RequestParam("file") MultipartFile file, Authentication authentication) {
        UserDetailsImpl principal = getPrincipal(authentication);
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Scope scope = scopeFor(principal);
        if (!scope.canManageUsers()) return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");

        if (file == null || file.isEmpty()) return ResponseEntity.badRequest().body("File trống");

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            int created = 0;
            StringBuilder errors = new StringBuilder();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String username = getCellString(row.getCell(0));
                String password = getCellString(row.getCell(1));
                String fullName = getCellString(row.getCell(2));
                String role = getCellString(row.getCell(3));
                String schoolCode = getCellString(row.getCell(4));
                String facultyCode = getCellString(row.getCell(5));

                if (username.isBlank()) continue;

                try {
                    if (userRepository.existsByUsername(username)) {
                        throw new IllegalArgumentException("Username already exists");
                    }
                    if (password.isBlank()) {
                        throw new IllegalArgumentException("Missing password");
                    }
                    if (role.isBlank()) {
                        throw new IllegalArgumentException("Missing role");
                    }
                    if (!isValidRole(role)) {
                        throw new IllegalArgumentException("Invalid role: " + role);
                    }

                    UpsertUserRequest req = new UpsertUserRequest();
                    req.setUsername(username);
                    req.setPassword(password);
                    req.setFullName(fullName.isBlank() ? null : fullName);
                    req.setRole(role);

                    if (!schoolCode.isBlank()) {
                        Long schoolId = schoolRepository.findByCode(schoolCode).map(School::getId)
                                .orElseThrow(() -> new IllegalArgumentException("School code not found: " + schoolCode));
                        req.setSchoolId(schoolId);
                    }
                    if (!facultyCode.isBlank()) {
                        Long facultyId = facultyRepository.findByCode(facultyCode).map(Faculty::getId)
                                .orElseThrow(() -> new IllegalArgumentException("Faculty code not found: " + facultyCode));
                        req.setFacultyId(facultyId);
                    }

                    User u = new User();
                    u.setUsername(username);
                    u.setPassword(passwordEncoder.encode(password));
                    u.setFullName(req.getFullName());
                    resolveAndApplyRoleScope(u, role, req, scope);
                    userRepository.save(u);
                    created++;
                } catch (Exception ex) {
                    if (!errors.isEmpty()) errors.append("\n");
                    errors.append("Row ").append(i + 1).append(": ").append(ex.getMessage());
                }
            }

            String msg = "Imported users: " + created;
            if (!errors.isEmpty()) msg = msg + "\nErrors:\n" + errors;
            return ResponseEntity.ok(msg);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Cannot read Excel: " + e.getMessage());
        }
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            String[] cols = {
                    "Username",
                    "Password",
                    "Full Name",
                    "Role (ADMIN|ADMIN_SCHOOL|ADMIN_FACULTY)",
                    "School Code (Optional)",
                    "Faculty Code (Optional)"
            };

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 28);

            Row sample1 = sheet.createRow(1);
            sample1.createCell(0).setCellValue("admin_demo");
            sample1.createCell(1).setCellValue("123456");
            sample1.createCell(2).setCellValue("Phenikaa Admin");
            sample1.createCell(3).setCellValue("ADMIN");

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("admin_school_demo");
            sample2.createCell(1).setCellValue("123456");
            sample2.createCell(2).setCellValue("Giáo vụ Trường");
            sample2.createCell(3).setCellValue("ADMIN_SCHOOL");
            sample2.createCell(4).setCellValue("PSC");

            Row sample3 = sheet.createRow(3);
            sample3.createCell(0).setCellValue("admin_faculty_demo");
            sample3.createCell(1).setCellValue("123456");
            sample3.createCell(2).setCellValue("Giáo vụ Khoa");
            sample3.createCell(3).setCellValue("ADMIN_FACULTY");
            sample3.createCell(5).setCellValue("PSC1");

            return ExcelTemplateUtil.toXlsxResponse(workbook, "User_Import_Template.xlsx");
        }
    }

    private static UserDetailsImpl getPrincipal(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl u) return u;
        return null;
    }

    private static boolean hasAuthority(UserDetailsImpl principal, String authority) {
        if (principal == null) return false;
        return principal.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private static boolean isValidRole(String role) {
        return List.of("ADMIN", "ADMIN_SCHOOL", "ADMIN_FACULTY").contains(role);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    private static String getCellString(Cell cell) {
        if (cell == null) return "";
        return DATA_FORMATTER.formatCellValue(cell).trim();
    }

    private Scope scopeFor(UserDetailsImpl principal) {
        boolean isAdmin = hasAuthority(principal, "ADMIN");
        boolean isAdminSchool = hasAuthority(principal, "ADMIN_SCHOOL");
        boolean isAdminFaculty = hasAuthority(principal, "ADMIN_FACULTY");

        if (isAdmin) return Scope.admin();
        if (isAdminSchool) return Scope.adminSchool(principal.getSchoolId());
        if (isAdminFaculty) return Scope.none();
        return Scope.none();
    }

    private void resolveAndApplyRoleScope(User user, String role, UpsertUserRequest request, Scope actorScope) {
        if (!actorScope.canAssignRole(role)) {
            throw new IllegalArgumentException("Cannot assign role: " + role);
        }

        user.setRole(role);
        user.setSchool(null);
        user.setFaculty(null);

        Long requestedSchoolId = request != null ? request.getSchoolId() : null;
        Long requestedFacultyId = request != null ? request.getFacultyId() : null;

        if ("ADMIN_SCHOOL".equals(role)) {
            Long schoolId = requestedSchoolId;
            if (actorScope.schoolId != null) schoolId = actorScope.schoolId;
            if (schoolId == null) throw new IllegalArgumentException("schoolId is required for ADMIN_SCHOOL");

            if (actorScope.schoolId != null && !actorScope.schoolId.equals(schoolId)) {
                throw new IllegalArgumentException("Out of scope schoolId");
            }

            School school = schoolRepository.findById(schoolId)
                    .orElseThrow(() -> new IllegalArgumentException("School not found"));
            user.setSchool(school);
            return;
        }

        if ("ADMIN_FACULTY".equals(role)) {
            Long facultyId = requestedFacultyId;
            if (actorScope.facultyId != null) facultyId = actorScope.facultyId;
            if (facultyId == null) throw new IllegalArgumentException("facultyId is required for ADMIN_FACULTY");

            Faculty faculty = facultyRepository.findById(facultyId)
                    .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

            if (actorScope.facultyId != null && !actorScope.facultyId.equals(facultyId)) {
                throw new IllegalArgumentException("Out of scope facultyId");
            }
            if (actorScope.schoolId != null) {
                Long facultySchoolId = faculty.getSchool() != null ? faculty.getSchool().getId() : null;
                if (facultySchoolId == null || !actorScope.schoolId.equals(facultySchoolId)) {
                    throw new IllegalArgumentException("Faculty is not in your school");
                }
            }

            user.setFaculty(faculty);
            return;
        }

        // ADMIN role: no scope binding required. If provided, accept optional linkage (still must respect actor scope)
        if (requestedSchoolId != null) {
            if (actorScope.schoolId != null && !actorScope.schoolId.equals(requestedSchoolId)) {
                throw new IllegalArgumentException("Out of scope schoolId");
            }
            School school = schoolRepository.findById(requestedSchoolId)
                    .orElseThrow(() -> new IllegalArgumentException("School not found"));
            user.setSchool(school);
        }
        if (requestedFacultyId != null) {
            if (actorScope.facultyId != null && !actorScope.facultyId.equals(requestedFacultyId)) {
                throw new IllegalArgumentException("Out of scope facultyId");
            }
            Faculty faculty = facultyRepository.findById(requestedFacultyId)
                    .orElseThrow(() -> new IllegalArgumentException("Faculty not found"));

            if (actorScope.schoolId != null) {
                Long facultySchoolId = faculty.getSchool() != null ? faculty.getSchool().getId() : null;
                if (facultySchoolId == null || !actorScope.schoolId.equals(facultySchoolId)) {
                    throw new IllegalArgumentException("Faculty is not in your school");
                }
            }
            user.setFaculty(faculty);
        }
    }

    @Data
    public static class UpsertUserRequest {
        private String username;
        private String password;
        private String fullName;
        private String role;
        private Long schoolId;
        private Long facultyId;
    }

    private static final class Scope {
        private final String kind;
        private final Long schoolId;
        private final Long facultyId;

        private Scope(String kind, Long schoolId, Long facultyId) {
            this.kind = kind;
            this.schoolId = schoolId;
            this.facultyId = facultyId;
        }

        static Scope admin() { return new Scope("ADMIN", null, null); }
        static Scope adminSchool(Long schoolId) { return new Scope("ADMIN_SCHOOL", schoolId, null); }
        static Scope adminFaculty(Long facultyId) { return new Scope("ADMIN_FACULTY", null, facultyId); }
        static Scope none() { return new Scope("NONE", null, null); }

        boolean canManageUsers() {
            return !"NONE".equals(kind);
        }

        boolean canAssignRole(String role) {
            if ("ADMIN".equals(kind)) return true;
            if ("ADMIN_SCHOOL".equals(kind)) return !"ADMIN".equals(role);
            if ("ADMIN_FACULTY".equals(kind)) return "ADMIN_FACULTY".equals(role);
            return false;
        }

        boolean canAccess(User target) {
            if ("ADMIN".equals(kind)) return true;
            if ("ADMIN_SCHOOL".equals(kind)) {
                Long targetSchoolId = target.getSchool() != null ? target.getSchool().getId() : null;
                Long targetFacultySchoolId = (target.getFaculty() != null && target.getFaculty().getSchool() != null)
                        ? target.getFaculty().getSchool().getId()
                        : null;
                return (schoolId != null) && (schoolId.equals(targetSchoolId) || schoolId.equals(targetFacultySchoolId));
            }
            if ("ADMIN_FACULTY".equals(kind)) {
                Long targetFacultyId = target.getFaculty() != null ? target.getFaculty().getId() : null;
                return facultyId != null && facultyId.equals(targetFacultyId);
            }
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class UserSummaryDto {
        private Long id;
        private String username;
        private String fullName;
        private String role;

        private Long facultyId;
        private String facultyName;

        private Long schoolId;
        private String schoolName;

        public static UserSummaryDto from(User user) {
            Long facultyId = user.getFaculty() != null ? user.getFaculty().getId() : null;
            String facultyName = user.getFaculty() != null ? user.getFaculty().getName() : null;

            Long schoolId = user.getSchool() != null ? user.getSchool().getId() : null;
            String schoolName = user.getSchool() != null ? user.getSchool().getName() : null;

            return new UserSummaryDto(
                    user.getId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getRole(),
                    facultyId,
                    facultyName,
                    schoolId,
                    schoolName
            );
        }
    }
}
