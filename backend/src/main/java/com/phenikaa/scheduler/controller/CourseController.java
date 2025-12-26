package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.service.CourseService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@CrossOrigin(origins = "http://localhost:5173")
public class CourseController {

    @Autowired private CourseService courseService;

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
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

            Row header = sheet.createRow(0);
            // Header tiếng Anh chi tiết
            String[] cols = {
                "Course Code", 
                "Course Name", 
                "Credits", 
                "Theory Credits", 
                "Practice Credits", 
                "Managing Faculty Code"
            };

            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);

            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Dữ liệu mẫu
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("CSE702011");
            sample.createCell(1).setCellValue("Java Programming");
            sample.createCell(2).setCellValue(3);
            sample.createCell(3).setCellValue(2);
            sample.createCell(4).setCellValue(1);
            sample.createCell(5).setCellValue("F_SE"); // Mã khoa quản lý

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Course_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}