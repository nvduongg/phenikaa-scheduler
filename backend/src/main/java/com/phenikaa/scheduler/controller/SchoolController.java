package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.service.SchoolService;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schools")
@CrossOrigin(origins = "http://localhost:5173")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    public ResponseEntity<List<School>> getAllSchools(Authentication authentication) {
        Long schoolId = getSchoolIdIfAdminSchool(authentication);
        if (schoolId != null) {
            return schoolService.getSchoolById(schoolId)
                    .map(school -> ResponseEntity.ok(Collections.singletonList(school)))
                    .orElse(ResponseEntity.ok(Collections.emptyList()));
        }
        return ResponseEntity.ok(schoolService.getAllSchools());
    }

    private Long getSchoolIdIfAdminSchool(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            return null;
        }
        boolean isAdminSchool = userDetails.getAuthorities().stream()
                .anyMatch(a -> "ADMIN_SCHOOL".equals(a.getAuthority()));
        return isAdminSchool ? userDetails.getSchoolId() : null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<School> getSchoolById(@PathVariable Long id) {
        return schoolService.getSchoolById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<School> createSchool(@RequestBody School school) {
        School created = schoolService.createSchool(school);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<School> updateSchool(@PathVariable Long id, @RequestBody School school) {
        return schoolService.updateSchool(id, school)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchool(@PathVariable Long id) {
        if (schoolService.deleteSchool(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importSchools(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File trống");
        return ResponseEntity.ok(schoolService.importSchoolsExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Schools"); // Sheet name ngắn gọn

            // Header tiếng Anh chuẩn
            String[] cols = {"School Code", "School Name"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 30);

            // Dữ liệu mẫu (Sample Data)
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("SCS");
            sample.createCell(1).setCellValue("School of Computer Science");

            return ExcelTemplateUtil.toXlsxResponse(workbook, "School_Import_Template.xlsx");
        }
    }
}