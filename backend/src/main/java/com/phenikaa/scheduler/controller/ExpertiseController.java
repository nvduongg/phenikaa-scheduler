package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.service.ExpertiseService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/expertise")
@CrossOrigin(origins = "http://localhost:5173")
public class ExpertiseController {

    private final ExpertiseService expertiseService;

    public ExpertiseController(ExpertiseService expertiseService) {
        this.expertiseService = expertiseService;
    }

    // API Import
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importExpertise(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(expertiseService.importExpertiseExcel(file));
    }

    // API Template
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Expertise");

            String[] cols = {"Lecturer Code", "Course Code"};
            
            // Style
            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 20);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("GV001");
            sample.createCell(1).setCellValue("CSE702011");

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Expertise_Import_Template.xlsx");
        }
    }
}