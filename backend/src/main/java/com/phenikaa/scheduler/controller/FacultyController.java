package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.service.FacultyService;
import com.phenikaa.scheduler.security.services.UserDetailsImpl; // Import UserDetailsImpl
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // Import Authentication
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/faculties")
@CrossOrigin(origins = "http://localhost:5173")
public class FacultyController {

    private final FacultyService facultyService;

    public FacultyController(FacultyService facultyService) {
        this.facultyService = facultyService;
    }

    @GetMapping
    public ResponseEntity<List<Faculty>> getAllFaculties(Authentication authentication) {
        Long schoolId = getSchoolIdIfAdminSchool(authentication);
        if (schoolId != null) {
            return ResponseEntity.ok(facultyService.getFacultiesBySchoolId(schoolId));
        }
        return ResponseEntity.ok(facultyService.getAllFaculties());
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
    public ResponseEntity<Faculty> getFacultyById(@PathVariable Long id) {
        return facultyService.getFacultyById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Faculty> createFaculty(@RequestBody Faculty faculty) {
        return ResponseEntity.ok(facultyService.createFaculty(faculty));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Faculty> updateFaculty(@PathVariable Long id, @RequestBody Faculty faculty) {
        return facultyService.updateFaculty(id, faculty).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long id) {
        if (facultyService.deleteFaculty(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
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

            // Header tiếng Anh
            String[] cols = {"Faculty Code", "Faculty Name", "School Code (Optional)"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 25);

            // Dữ liệu mẫu
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("F_SE");
            sample.createCell(1).setCellValue("Faculty of Software Engineering");
            sample.createCell(2).setCellValue("SCS"); // Mã trường phải khớp với School Code đã import

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Faculty_Import_Template.xlsx");
        }
    }
}