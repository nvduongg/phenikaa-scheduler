package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.Room;
import com.phenikaa.scheduler.service.RoomService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAllRooms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        Room created = roomService.createRoom(room);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room room) {
        return roomService.updateRoom(id, room).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        if (roomService.deleteRoom(id)) return ResponseEntity.noContent().build();
        return ResponseEntity.notFound().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> importRooms(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("File is empty");
        return ResponseEntity.ok(roomService.importRoomsExcel(file));
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Rooms");

            String[] cols = {"Room Name", "Capacity", "Type (THEORY / PC / LAB / HALL / ONLINE)"};

            CellStyle style = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
            ExcelTemplateUtil.createHeaderRow(sheet, cols, style, 30);

            // Sample Data (English)
            Object[][] data = {
                {"A2-301", 60, "THEORY"},
                {"A6-501 (PC)", 45, "PC"},
                {"Lab Hóa", 40, "LAB"},
                {"Football Field", 100, "HALL"},
                {"ONLINE-Teams", 500, "ONLINE"}
            };

            int rowIdx = 1;
            for (Object[] rowData : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue((String) rowData[0]);
                row.createCell(1).setCellValue((Integer) rowData[1]);
                row.createCell(2).setCellValue((String) rowData[2]);
            }

            return ExcelTemplateUtil.toXlsxResponse(workbook, "Room_Import_Template.xlsx");
        }
    }
}