package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.FacultyRepository;
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

    public List<Course> getAllCourses() {
        return courseRepo.findAll();
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
                    course.setCredits(parseIntSafe(row.getCell(2)));
                    course.setTheoryCredits(parseIntSafe(row.getCell(3)));
                    course.setPracticeCredits(parseIntSafe(row.getCell(4)));

                    // 3. Xử lý Khoa quản lý
                    String facultyCode = getCellValue(row.getCell(5));
                    Optional<Faculty> facultyOpt = facultyRepo.findByCode(facultyCode);

                    if (facultyOpt.isPresent()) {
                        Faculty fac = facultyOpt.get();

                        // --- SỬA TẠI ĐÂY ---
                        course.setManagingFaculty(fac); // Gán khoa chuyên môn
                        course.setFaculty(fac);         // Gán khoa hành chính (FIX LỖI NULL faculty_id)
                        // -------------------

                        courseRepo.save(course);
                        successCount++;
                    } else {
                        errors.add("Dòng " + (i + 1) + ": Không tìm thấy Mã khoa '" + facultyCode + "'");
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
    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    // Helper: Parse Int an toàn
    private int parseIntSafe(Cell cell) {
        try {
            String val = getCellValue(cell);
            return val.isEmpty() ? 0 : (int) Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}