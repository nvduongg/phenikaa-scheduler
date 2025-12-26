package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.AdministrativeClass;
import com.phenikaa.scheduler.service.AdministrativeClassService;
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
@RequestMapping("/api/v1/admin-classes")
@CrossOrigin(origins = "http://localhost:5173")
public class AdministrativeClassController {

    @Autowired private AdministrativeClassService adminClassService;

    @GetMapping
    public ResponseEntity<List<AdministrativeClass>> getAllClasses() {
        return ResponseEntity.ok(adminClassService.getAllClasses());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importClasses(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(adminClassService.importClassesExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("AdminClasses");

            Row header = sheet.createRow(0);
            String[] cols = {"Class Name", "Major Code", "Cohort Name", "Size (Students)"};

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

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("K17-CNTT-01");
            sample.createCell(1).setCellValue("7480201"); // Mã ngành phải tồn tại
            sample.createCell(2).setCellValue("K17");     // Mã khóa phải tồn tại
            sample.createCell(3).setCellValue(65);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Admin_Class_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}