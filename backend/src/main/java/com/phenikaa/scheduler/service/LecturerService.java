package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.LecturerRepository;
import com.phenikaa.scheduler.security.SecurityUtils;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LecturerService {

    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private FacultyRepository facultyRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private SecurityUtils securityUtils; // Inject tiện ích vừa viết

    public List<Lecturer> getAllLecturers() {
        UserDetailsImpl user = securityUtils.getCurrentUser();
        String role = user.getAuthorities().iterator().next().getAuthority();

        // 1. ADMIN (Đại học): Thấy toàn bộ giảng viên
        if (role.equals("ADMIN")) {
            return lecturerRepo.findAll();
        }

        // 2. ADMIN_SCHOOL (Trường thành viên): Thấy giảng viên của tất cả khoa con
        if (role.equals("ADMIN_SCHOOL") && user.getSchoolId() != null) {
            return lecturerRepo.findByFaculty_School_Id(user.getSchoolId());
        }

        // 3. ADMIN_FACULTY (Khoa): Chỉ thấy giảng viên khoa mình
        if (role.equals("ADMIN_FACULTY") && user.getFacultyId() != null) {
            return lecturerRepo.findByFacultyId(user.getFacultyId());
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("null")
    public java.util.Optional<Lecturer> getLecturerById(Long id) {
        return lecturerRepo.findById(id);
    }

    @SuppressWarnings("null")
    public Lecturer createLecturer(Lecturer lecturer) {
        // attach faculty if provided
        if (lecturer.getFaculty() != null) {
            if (lecturer.getFaculty().getId() != null) {
                facultyRepo.findById(lecturer.getFaculty().getId()).ifPresent(lecturer::setFaculty);
            } else if (lecturer.getFaculty().getCode() != null) {
                facultyRepo.findByCode(lecturer.getFaculty().getCode()).ifPresent(lecturer::setFaculty);
            }
        }
        // attach courses if provided
        if (lecturer.getTeachingCourses() != null && !lecturer.getTeachingCourses().isEmpty()) {
            List<Course> managedCourses = new ArrayList<>();
            for (Course c : lecturer.getTeachingCourses()) {
                if (c.getId() != null) {
                    courseRepo.findById(c.getId()).ifPresent(managedCourses::add);
                } else if (c.getCourseCode() != null) {
                    courseRepo.findByCourseCode(c.getCourseCode()).ifPresent(managedCourses::add);
                }
            }
            lecturer.setTeachingCourses(managedCourses);
        }
        return lecturerRepo.save(lecturer);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Lecturer> updateLecturer(Long id, Lecturer updated) {
        return lecturerRepo.findById(id).map(l -> {
            l.setLecturerCode(updated.getLecturerCode());
            l.setFullName(updated.getFullName());
            l.setEmail(updated.getEmail());
            l.setTitle(updated.getTitle());
            
            if (updated.getFaculty() != null) {
                if (updated.getFaculty().getId() != null) facultyRepo.findById(updated.getFaculty().getId()).ifPresent(l::setFaculty);
                else if (updated.getFaculty().getCode() != null) facultyRepo.findByCode(updated.getFaculty().getCode()).ifPresent(l::setFaculty);
            }
            
            if (updated.getTeachingCourses() != null) {
                List<Course> managedCourses = new ArrayList<>();
                for (Course c : updated.getTeachingCourses()) {
                    if (c.getId() != null) {
                        courseRepo.findById(c.getId()).ifPresent(managedCourses::add);
                    } else if (c.getCourseCode() != null) {
                        courseRepo.findByCourseCode(c.getCourseCode()).ifPresent(managedCourses::add);
                    }
                }
                l.setTeachingCourses(managedCourses);
            }
            
            return lecturerRepo.save(l);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteLecturer(Long id) {
        if (lecturerRepo.existsById(id)) {
            lecturerRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public String importLecturersExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Lecturer Code | Col 1: Full Name | Col 2: Email | Col 3: Faculty Code
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String email = getCellValue(row.getCell(2));
                    String facultyCode = getCellValue(row.getCell(3));

                    if (code.isEmpty() || name.isEmpty()) continue;

                    // 1. Validate Faculty
                    Optional<Faculty> facultyOpt = facultyRepo.findByCode(facultyCode);
                    if (facultyOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Faculty Code '" + facultyCode + "' not found.");
                        continue;
                    }

                    // 2. Create/Update Lecturer
                    Lecturer lecturer = lecturerRepo.findByLecturerCode(code).orElse(new Lecturer());
                    lecturer.setLecturerCode(code);
                    lecturer.setFullName(name);
                    lecturer.setEmail(email);
                    lecturer.setFaculty(facultyOpt.get());

                    lecturerRepo.save(lecturer);
                    successCount++;

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + " Error: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "File Error: " + e.getMessage();
        }
        return "Import completed! Success: " + successCount + ". Errors: " + errors.size() + "\n" + errors;
    }

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    public void updateExpertise(Long lecturerId, List<String> courseCodes) {
        @SuppressWarnings("null")
        Lecturer lecturer = lecturerRepo.findById(lecturerId)
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));
        
        // Tìm các môn học dựa trên list code gửi lên
        List<Course> newCourses = courseRepo.findByCourseCodeIn(courseCodes);
        
        // Cập nhật danh sách (Ghi đè list cũ bằng list mới)
        lecturer.setTeachingCourses(newCourses);
        lecturerRepo.save(lecturer);
    }
}