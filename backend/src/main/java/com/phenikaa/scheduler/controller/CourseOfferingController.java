package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.service.AutoAssignService;
import com.phenikaa.scheduler.service.CourseOfferingService;
import com.phenikaa.scheduler.service.SchedulerService;

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
@RequestMapping("/api/v1/offerings")
@CrossOrigin(origins = "http://localhost:5173") // Cấu hình cho phép React (Vite) truy cập
public class CourseOfferingController {

    @Autowired
    private CourseOfferingService offeringService;
    @Autowired 
    private AutoAssignService autoAssignService;
    @Autowired 
    private SchedulerService schedulerService;

    // API 1: Lấy danh sách toàn bộ kế hoạch mở lớp
    @GetMapping
    public ResponseEntity<List<CourseOffering>> getAllOfferings() {
        return ResponseEntity.ok(offeringService.getAllOfferings());
    }

    // API 2: Import dữ liệu từ file Excel (.xlsx)
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importOfferings(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Vui lòng chọn file Excel để upload!");
        }

        // Gọi service xử lý file Excel
        // Lưu ý: Đảm bảo bạn đã update method importCourseOfferingsExcel trong Service như bước trước
        String result = offeringService.importCourseOfferingsExcel(file);
        
        return ResponseEntity.ok(result);
    }

    // API 3: Tải file Excel mẫu (Template)
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Offering Plan");

            Row header = sheet.createRow(0);
            // Header tiếng Anh chuẩn cho Input
            String[] cols = {
                "Class Code (Unique)", 
                "Course Code", 
                "Planned Size", 
                "Target Classes (e.g., K17-CNTT-01)", 
                "Fixed Lecturer Code (Optional)"
            };

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

            // Sample Data
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("2025_JAVA_01");
            sample.createCell(1).setCellValue("CSE702011");
            sample.createCell(2).setCellValue(60);
            sample.createCell(3).setCellValue("K17-CNTT-01; K17-CNTT-02");
            sample.createCell(4).setCellValue("GV001"); // Có thể để trống nếu muốn máy tự xếp GV

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Offering_Plan_Template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }

    // API Trigger Auto Assign
    @PostMapping("/auto-assign-lecturers")
    public ResponseEntity<String> autoAssignLecturers() {
        String result = autoAssignService.autoAssignLecturers();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/generate-schedule")
    public ResponseEntity<String> generateSchedule() {
        long startTime = System.currentTimeMillis();
        String result = schedulerService.generateSchedule();
        long duration = System.currentTimeMillis() - startTime;
        return ResponseEntity.ok(result + " (Time: " + duration + "ms)");
    }
}