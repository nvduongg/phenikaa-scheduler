package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.service.CourseService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "http://localhost:5173")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Course> getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        Course created = courseService.createCourse(course);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        return courseService.updateCourse(id, course).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable Long id) {
        if (courseService.deleteCourse(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCourses(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File không được để trống");
        return ResponseEntity.ok(courseService.importCoursesExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Courses"); // Tên sheet ngắn gọn

            // Header tiếng Anh chi tiết
            String[] cols = {
                "Course Code", 
                "Course Name", 
                "Credits", 
                "Theory Credits", 
                "Practice Credits", 
                "Managing Faculty Code",
                "Managing School Code"
            };

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 20);

            // Dữ liệu mẫu
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("CSE702011");
            sample.createCell(1).setCellValue("Java Programming");
            sample.createCell(2).setCellValue(3.0);
            sample.createCell(3).setCellValue(2.0);
            sample.createCell(4).setCellValue(1.0);
            sample.createCell(5).setCellValue("F_SE"); // Mã khoa quản lý
            sample.createCell(6).setCellValue(""); // Mã trường (để trống nếu đã có khoa)

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("PHX101");
            sample2.createCell(1).setCellValue("Phenikaa Life");
            sample2.createCell(2).setCellValue(2.5); // Ví dụ số lẻ
            sample2.createCell(3).setCellValue(1.5);
            sample2.createCell(4).setCellValue(1.0);
            sample2.createCell(5).setCellValue("");
            sample2.createCell(6).setCellValue("PHX"); // Mã trường quản lý

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Course_Import_Template.xlsx");
        }
    }
}