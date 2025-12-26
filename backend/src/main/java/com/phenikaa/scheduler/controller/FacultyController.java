package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.service.FacultyService;
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
@RequestMapping("/api/v1/faculties")
@CrossOrigin(origins = "http://localhost:5173")
public class FacultyController {

    @Autowired private FacultyService facultyService;

    @GetMapping
    public ResponseEntity<List<Faculty>> getAllFaculties() {
        return ResponseEntity.ok(facultyService.getAllFaculties());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importFaculties(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File trống");
        return ResponseEntity.ok(facultyService.importFacultiesExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Faculties");

            Row header = sheet.createRow(0);
            // Header tiếng Anh
            String[] cols = {"Faculty Code", "Faculty Name", "School Code (Optional)"};

            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);

            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 25 * 256);
            }

            // Dữ liệu mẫu
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("F_SE");
            sample.createCell(1).setCellValue("Faculty of Software Engineering");
            sample.createCell(2).setCellValue("SCS"); // Mã trường phải khớp với School Code đã import

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Faculty_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}