package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.CurriculumDetail;
import com.phenikaa.scheduler.service.CurriculumDetailService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curriculum-details")
@CrossOrigin(origins = "http://localhost:5173")
public class CurriculumDetailController {

    private final CurriculumDetailService detailService;

    public CurriculumDetailController(CurriculumDetailService detailService) {
        this.detailService = detailService;
    }

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

            String[] cols = {"Curriculum Name", "Course Code", "Semester Index (e.g. 1 or 1,2)"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 25);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("CTĐT K17 CNTT");
            sample.createCell(1).setCellValue("CSE702011");
            sample.createCell(2).setCellValue("3"); // Học kỳ 3

            Row sample2 = sheet.createRow(2);
            sample2.createCell(0).setCellValue("CTĐT K17 CNTT");
            sample2.createCell(1).setCellValue("PHX101");
            sample2.createCell(2).setCellValue("1,2"); // Học kỳ 1 hoặc 2

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Roadmap_Import_Template.xlsx");
        }
    }
}