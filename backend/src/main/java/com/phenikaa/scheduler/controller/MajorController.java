package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Major;
import com.phenikaa.scheduler.service.MajorService;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/majors")
@CrossOrigin(origins = "http://localhost:5173")
public class MajorController {

    private final MajorService majorService;

    public MajorController(MajorService majorService) {
        this.majorService = majorService;
    }

    @GetMapping
    public ResponseEntity<List<Major>> getAllMajors(Authentication authentication) {
        Long schoolId = getSchoolIdIfAdminSchool(authentication);
        if (schoolId != null) {
            return ResponseEntity.ok(majorService.getMajorsBySchoolId(schoolId));
        }
        return ResponseEntity.ok(majorService.getAllMajors());
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
    public ResponseEntity<Major> getMajorById(@PathVariable Long id) {
        return majorService.getMajorById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Major> createMajor(@RequestBody Major major) {
        return ResponseEntity.ok(majorService.createMajor(major));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Major> updateMajor(@PathVariable Long id, @RequestBody Major major) {
        return majorService.updateMajor(id, major).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMajor(@PathVariable Long id) {
        if (majorService.deleteMajor(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importMajors(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(majorService.importMajorsExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Majors");

            String[] cols = {"Major Code", "Major Name", "Faculty Code"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 25);

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("7480201"); // Mã ngành chuẩn Bộ GD
            sample.createCell(1).setCellValue("Information Technology");
            sample.createCell(2).setCellValue("F_SE"); // Mã Khoa phải tồn tại

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Major_Import_Template.xlsx");
        }
    }
}