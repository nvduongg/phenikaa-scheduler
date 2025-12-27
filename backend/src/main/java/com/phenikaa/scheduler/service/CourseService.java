package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.SchoolRepository;
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
public class CourseService {

    @Autowired private CourseRepository courseRepo;
    @Autowired private FacultyRepository facultyRepo;
    @Autowired private SchoolRepository schoolRepo;

    public List<Course> getAllCourses() {
        return courseRepo.findAll();
    }

    @SuppressWarnings("null")
    public java.util.Optional<Course> getCourseById(Long id) {
        return courseRepo.findById(id);
    }

    @SuppressWarnings("null")
    public Course createCourse(Course course) {
        if (course.getManagingFaculty() != null) {
            if (course.getManagingFaculty().getId() != null) {
                facultyRepo.findById(course.getManagingFaculty().getId()).ifPresent(course::setManagingFaculty);
            } else if (course.getManagingFaculty().getCode() != null) {
                facultyRepo.findByCode(course.getManagingFaculty().getCode()).ifPresent(course::setManagingFaculty);
            }
            // also set administrative faculty if null
            if (course.getFaculty() == null) course.setFaculty(course.getManagingFaculty());
            course.setSchool(null);
        } else if (course.getSchool() != null) {
            if (course.getSchool().getId() != null) {
                schoolRepo.findById(course.getSchool().getId()).ifPresent(course::setSchool);
            } else if (course.getSchool().getCode() != null) {
                schoolRepo.findByCode(course.getSchool().getCode()).ifPresent(course::setSchool);
            }
            course.setFaculty(null);
            course.setManagingFaculty(null);
        }
        return courseRepo.save(course);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Course> updateCourse(Long id, Course updated) {
        return courseRepo.findById(id).map(c -> {
            c.setCourseCode(updated.getCourseCode());
            c.setName(updated.getName());
            c.setCredits(updated.getCredits());
            c.setTheoryCredits(updated.getTheoryCredits());
            c.setPracticeCredits(updated.getPracticeCredits());
            
            if (updated.getManagingFaculty() != null) {
                if (updated.getManagingFaculty().getId() != null) facultyRepo.findById(updated.getManagingFaculty().getId()).ifPresent(c::setManagingFaculty);
                else if (updated.getManagingFaculty().getCode() != null) facultyRepo.findByCode(updated.getManagingFaculty().getCode()).ifPresent(c::setManagingFaculty);
                
                if (c.getFaculty() == null) c.setFaculty(c.getManagingFaculty());
                c.setSchool(null);
            } else if (updated.getSchool() != null) {
                if (updated.getSchool().getId() != null) schoolRepo.findById(updated.getSchool().getId()).ifPresent(c::setSchool);
                else if (updated.getSchool().getCode() != null) schoolRepo.findByCode(updated.getSchool().getCode()).ifPresent(c::setSchool);
                
                c.setFaculty(null);
                c.setManagingFaculty(null);
            }
            return courseRepo.save(c);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteCourse(Long id) {
        if (courseRepo.existsById(id)) {
            courseRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public String importCoursesExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Duyệt từ dòng 1 (bỏ Header dòng 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Cấu trúc cột Excel:
                    // 0: Mã HP | 1: Tên HP | 2: Số TC | 3: TC LT | 4: TC TH | 5: Mã Khoa Quản lý
                    
                    String courseCode = getCellValue(row.getCell(0));
                    String courseName = getCellValue(row.getCell(1));
                    
                    // Nếu thiếu Mã hoặc Tên thì bỏ qua
                    if (courseCode.isEmpty() || courseName.isEmpty()) continue;

                    // 1. Tìm hoặc tạo mới Course
                    Course course = courseRepo.findByCourseCode(courseCode).orElse(new Course());
                    course.setCourseCode(courseCode);
                    course.setName(courseName);

                    // 2. Parse các chỉ số tín chỉ
                    course.setCredits(parseDoubleSafe(row.getCell(2)));
                    course.setTheoryCredits(parseDoubleSafe(row.getCell(3)));
                    course.setPracticeCredits(parseDoubleSafe(row.getCell(4)));

                    // 3. Xử lý Khoa/Trường quản lý
                    String facultyCode = getCellValue(row.getCell(5));
                    String schoolCode = getCellValue(row.getCell(6));

                    if (!facultyCode.isEmpty()) {
                        Optional<Faculty> facultyOpt = facultyRepo.findByCode(facultyCode);
                        if (facultyOpt.isPresent()) {
                            Faculty fac = facultyOpt.get();
                            course.setManagingFaculty(fac);
                            course.setFaculty(fac);
                            course.setSchool(null);
                            courseRepo.save(course);
                            successCount++;
                        } else {
                            errors.add("Dòng " + (i + 1) + ": Không tìm thấy Mã khoa '" + facultyCode + "'");
                        }
                    } else if (!schoolCode.isEmpty()) {
                        Optional<com.phenikaa.scheduler.model.School> schoolOpt = schoolRepo.findByCode(schoolCode);
                        if (schoolOpt.isPresent()) {
                            course.setSchool(schoolOpt.get());
                            course.setManagingFaculty(null);
                            course.setFaculty(null);
                            courseRepo.save(course);
                            successCount++;
                        } else {
                            errors.add("Dòng " + (i + 1) + ": Không tìm thấy Mã trường '" + schoolCode + "'");
                        }
                    } else {
                        errors.add("Dòng " + (i + 1) + ": Phải nhập Mã Khoa hoặc Mã Trường quản lý");
                    }

                } catch (Exception ex) {
                    errors.add("Lỗi dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "Lỗi đọc file: " + e.getMessage();
        }
        return "Import xong! Thành công: " + successCount + ". Lỗi: " + errors.size() + "\n" + errors;
    }

    // Helper: Lấy string từ cell
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    // Helper: Parse Double an toàn
    private Double parseDoubleSafe(Cell cell) {
        try {
            String val = getCellValue(cell);
            return val.isEmpty() ? 0.0 : Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}