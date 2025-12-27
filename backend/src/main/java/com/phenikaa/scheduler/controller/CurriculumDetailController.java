package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.CurriculumDetail;
import com.phenikaa.scheduler.service.CurriculumDetailService;
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
@RequestMapping("/api/v1/curriculum-details")
@CrossOrigin(origins = "http://localhost:5173")
public class CurriculumDetailController {

    @Autowired private CurriculumDetailService detailService;

    @GetMapping
    public ResponseEntity<List<CurriculumDetail>> getAllDetails() {
        return ResponseEntity.ok(detailService.getAllDetails());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importDetails(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(detailService.importDetailsExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Roadmap");

            Row header = sheet.createRow(0);
            String[] cols = {"Curriculum Name", "Course Code", "Semester Index (e.g. 1 or 1,2)"};

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

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("CTĐT K17 CNTT");
            sample.createCell(1).setCellValue("CSE702011");
            sample.createCell(2).setCellValue("3"); // Học kỳ 3

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("CTĐT K17 CNTT");
            sample2.createCell(1).setCellValue("PHX101");
            sample2.createCell(2).setCellValue("1,2"); // Học kỳ 1 hoặc 2

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Roadmap_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}