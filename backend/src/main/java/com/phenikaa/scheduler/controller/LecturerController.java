package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.service.LecturerService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lecturers")
@CrossOrigin(origins = "http://localhost:5173")
public class LecturerController {

    private final LecturerService lecturerService;

    public LecturerController(LecturerService lecturerService) {
        this.lecturerService = lecturerService;
    }

    @GetMapping
    public ResponseEntity<List<Lecturer>> getAllLecturers() {
        return ResponseEntity.ok(lecturerService.getAllLecturers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lecturer> getLecturerById(@PathVariable Long id) {
        return lecturerService.getLecturerById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Lecturer> createLecturer(@RequestBody Lecturer lecturer) {
        return ResponseEntity.ok(lecturerService.createLecturer(lecturer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lecturer> updateLecturer(@PathVariable Long id, @RequestBody Lecturer lecturer) {
        return lecturerService.updateLecturer(id, lecturer).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLecturer(@PathVariable Long id) {
        if (lecturerService.deleteLecturer(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importLecturers(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(lecturerService.importLecturersExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Lecturers");

            String[] cols = {"Lecturer Code", "Full Name", "Email", "Faculty Code"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 25);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("GV001");
            sample.createCell(1).setCellValue("Nguyen Van A");
            sample.createCell(2).setCellValue("a.nguyenvan@phenikaa-uni.edu.vn");
            sample.createCell(3).setCellValue("F_SE"); // Mã khoa

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Lecturer_Import_Template.xlsx");
        }
    }

    @PutMapping("/{id}/expertise")
    public ResponseEntity<String> updateExpertise(@PathVariable Long id, @RequestBody List<String> courseCodes) {
        lecturerService.updateExpertise(id, courseCodes);
        return ResponseEntity.ok("Expertise updated successfully");
    }
}