package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.service.AutoAssignService;
import com.phenikaa.scheduler.service.CourseOfferingService;
import com.phenikaa.scheduler.service.SchedulerService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/offerings")
@CrossOrigin(origins = "http://localhost:5173")
public class CourseOfferingController {

    private final CourseOfferingService offeringService;
    private final AutoAssignService autoAssignService;
    private final SchedulerService schedulerService;

    public CourseOfferingController(
            CourseOfferingService offeringService,
            AutoAssignService autoAssignService,
            SchedulerService schedulerService
    ) {
        this.offeringService = offeringService;
        this.autoAssignService = autoAssignService;
        this.schedulerService = schedulerService;
    }

    // API 1: Lấy danh sách toàn bộ kế hoạch mở lớp
    @GetMapping
    public ResponseEntity<List<CourseOffering>> getAllOfferings() {
        return ResponseEntity.ok(offeringService.getAllOfferings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseOffering> getOfferingById(@PathVariable Long id) {
        return offeringService.getOfferingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CourseOffering> createOffering(@RequestBody CourseOffering offering) {
        CourseOffering created = offeringService.createOffering(offering);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseOffering> updateOffering(@PathVariable Long id, @RequestBody CourseOffering offering) {
        return offeringService.updateOffering(id, offering)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOffering(@PathVariable Long id) {
        if (offeringService.deleteOffering(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/lecturer")
    public ResponseEntity<CourseOffering> assignLecturer(@PathVariable Long id, @RequestParam(required = false) Long lecturerId) {
        return ResponseEntity.ok(offeringService.assignLecturer(id, lecturerId));
    }

    // API 2: Import dữ liệu từ file Excel (.xlsx)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importOfferings(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn file Excel để upload!");
        }
        // Service đã được cập nhật logic 2 vòng lặp (Parent trước, Child sau)
        String result = offeringService.importCourseOfferingsExcel(file);
        return ResponseEntity.ok(result);
    }

    // API 3: Tải file Excel mẫu (CẬP NHẬT CẤU TRÚC MỚI)
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Offering Plan");

            
            // --- CẬP NHẬT HEADER (7 CỘT) ---
            String[] cols = {
                "Class Code (Unique)",       // Col 0
                "Course Code",               // Col 1
                "Planned Size",              // Col 2
                "Target Classes",            // Col 3
                "Fixed Lecturer (Optional)", // Col 4
                "Type (LT / TH / ALL / ELN / PC) (Optional)", // Col 5
                "Parent Code (Optional)"      // Col 6
            };

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 25);

            // --- DỮ LIỆU MẪU (SAMPLE DATA) ---
            
            // Mẫu 1: Lớp Lý thuyết (Lớp Mẹ)
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("2025_JAVA_01");
            row1.createCell(1).setCellValue("CSE702011");
            row1.createCell(2).setCellValue(80);
            row1.createCell(3).setCellValue("K17-CNTT-01");
            row1.createCell(4).setCellValue("");
            row1.createCell(5).setCellValue("LT"); // Đánh dấu là Lý thuyết
            row1.createCell(6).setCellValue("");   // LT không có cha

            // Mẫu 2: Lớp Thực hành 1 (Lớp Con)
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("2025_JAVA_01.1");
            row2.createCell(1).setCellValue("CSE702011");
            row2.createCell(2).setCellValue(40);
            row2.createCell(3).setCellValue("K17-CNTT-01");
            row2.createCell(4).setCellValue("");
            row2.createCell(5).setCellValue("TH"); // Đánh dấu là Thực hành
            row2.createCell(6).setCellValue("2025_JAVA_01"); // TRỎ VỀ MÃ CỦA DÒNG 1

            // Mẫu 3: Lớp Thực hành 2 (Lớp Con)
            Row row3 = sheet.createRow(3);
            row3.createCell(0).setCellValue("2025_JAVA_01.2");
            row3.createCell(1).setCellValue("CSE702011");
            row3.createCell(2).setCellValue(40);
            row3.createCell(3).setCellValue("K17-CNTT-01");
            row3.createCell(4).setCellValue("");
            row3.createCell(5).setCellValue("TH");
            row3.createCell(6).setCellValue("2025_JAVA_01"); // TRỎ VỀ MÃ CỦA DÒNG 1

            // Mẫu 3.1: Lớp học phòng máy (không cần LT/TH)
            Row row31 = sheet.createRow(4);
            row31.createCell(0).setCellValue("2025_PYTHON_PC_01");
            row31.createCell(1).setCellValue("CSE702012");
            row31.createCell(2).setCellValue(45);
            row31.createCell(3).setCellValue("K17-CNTT-01");
            row31.createCell(4).setCellValue("");
            row31.createCell(5).setCellValue("PC"); // ép phòng LAB/PC
            row31.createCell(6).setCellValue("");
            
            // Mẫu 4: Môn Online/Đại trà (Gộp chung)
            Row row4 = sheet.createRow(5);
            row4.createCell(0).setCellValue("2025_LAW_01");
            row4.createCell(1).setCellValue("LAW101");
            row4.createCell(2).setCellValue(200);
            row4.createCell(3).setCellValue("K17-ALL");
            row4.createCell(4).setCellValue("");
            row4.createCell(5).setCellValue("ELN"); // Hoặc ALL
            row4.createCell(6).setCellValue("");

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Offering_Plan_Template.xlsx");
        }
    }

    // API Trigger Auto Assign
    @PostMapping("/auto-assign-lecturers")
    public ResponseEntity<String> autoAssignLecturers() {
        String result = autoAssignService.autoAssignLecturers();
        return ResponseEntity.ok(result);
    }

    // API Trigger Scheduling (Always uses Genetic Algorithm)
    @PostMapping("/generate-schedule")
    public ResponseEntity<String> generateSchedule() {
        long startTime = System.currentTimeMillis();
        String result = schedulerService.generateSchedule(null);
        long duration = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(result + " (Time: " + duration + "ms)");
    }
}