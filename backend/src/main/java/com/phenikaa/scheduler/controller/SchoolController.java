package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.service.SchoolService;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schools")
@CrossOrigin(origins = "http://localhost:5173")
public class SchoolController {

    @Autowired private SchoolService schoolService;

    @GetMapping
    public ResponseEntity<List<School>> getAllSchools(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl) {
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            // Nếu là ADMIN_SCHOOL, chỉ trả về trường của họ
            if (userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN_SCHOOL"))) {
                if (userDetails.getSchoolId() != null) {
                    return schoolService.getSchoolById(userDetails.getSchoolId())
                            .map(school -> ResponseEntity.ok(Collections.singletonList(school)))
                            .orElse(ResponseEntity.ok(Collections.emptyList()));
                }
            }
        }
        return ResponseEntity.ok(schoolService.getAllSchools());
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

            Row header = sheet.createRow(0);
            // Header tiếng Anh chuẩn
            String[] cols = {"School Code", "School Name"};

            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);

            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(style);
                sheet.setColumnWidth(i, 30 * 256);
            }

            // Dữ liệu mẫu (Sample Data)
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("SCS");
            sample.createCell(1).setCellValue("School of Computer Science");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=School_Import_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}