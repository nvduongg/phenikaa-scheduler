package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Curriculum;
import com.phenikaa.scheduler.service.CurriculumService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/curricula")
@CrossOrigin(origins = "http://localhost:5173")
public class CurriculumController {

    private final CurriculumService curriculumService;

    public CurriculumController(CurriculumService curriculumService) {
        this.curriculumService = curriculumService;
    }

    @GetMapping
    public ResponseEntity<List<Curriculum>> getAllCurricula() {
        return ResponseEntity.ok(curriculumService.getAllCurricula());
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCurricula(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(curriculumService.importCurriculaExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Curricula");

            String[] cols = {"Curriculum Name", "Major Code", "Cohort Name"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 30);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("CTĐT K17 CNTT");
            sample.createCell(1).setCellValue("7480201"); // Mã ngành
            sample.createCell(2).setCellValue("K17");     // Mã khóa

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Curriculum_Import_Template.xlsx");
        }
    }
}