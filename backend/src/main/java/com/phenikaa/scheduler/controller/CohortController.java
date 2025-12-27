package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Cohort;
import com.phenikaa.scheduler.service.CohortService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cohorts")
@CrossOrigin(origins = "http://localhost:5173")
public class CohortController {

    private final CohortService cohortService;

    public CohortController(CohortService cohortService) {
        this.cohortService = cohortService;
    }

    @GetMapping
    public ResponseEntity<List<Cohort>> getAllCohorts() {
        return ResponseEntity.ok(cohortService.getAllCohorts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cohort> getCohortById(@PathVariable Long id) {
        return cohortService.getCohortById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Cohort> createCohort(@RequestBody Cohort cohort) {
        return ResponseEntity.ok(cohortService.createCohort(cohort));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cohort> updateCohort(@PathVariable Long id, @RequestBody Cohort cohort) {
        return cohortService.updateCohort(id, cohort).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCohort(@PathVariable Long id) {
        if (cohortService.deleteCohort(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importCohorts(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(cohortService.importCohortsExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Cohorts");

            String[] cols = {"Cohort Name", "Start Year", "End Year"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 20);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("K17");
            sample.createCell(1).setCellValue(2023);
            sample.createCell(2).setCellValue(2027);

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Cohort_Import_Template.xlsx");
        }
    }
}