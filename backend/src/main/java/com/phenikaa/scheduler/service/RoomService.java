package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Room;
import com.phenikaa.scheduler.repository.RoomRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class RoomService {

    @Autowired private RoomRepository roomRepo;

    public List<Room> getAllRooms() {
        return roomRepo.findAll();
    }

    @SuppressWarnings("null")
    public java.util.Optional<Room> getRoomById(Long id) {
        return roomRepo.findById(id);
    }

    @SuppressWarnings("null")
    public Room createRoom(Room room) {
        // Auto-detect room type if not provided or if name suggests PC
        if (room.getType() == null || room.getType().isEmpty()) {
            room.setType(inferRoomType(room.getName()));
        } else {
            // Even if type is provided, override to PC if name contains PC or A6
            String detectedType = inferRoomType(room.getName());
            if ("PC".equals(detectedType) && room.getName() != null) {
                String upperName = room.getName().toUpperCase();
                if (upperName.contains("PC") || upperName.contains("A6")) {
                    room.setType("PC");
                }
            }
        }
        return roomRepo.save(room);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Room> updateRoom(Long id, Room updated) {
        return roomRepo.findById(id).map(r -> {
            r.setName(updated.getName());
            r.setCapacity(updated.getCapacity());
            
            // Auto-detect room type based on new name
            String detectedType = inferRoomType(updated.getName());
            if (updated.getType() == null || updated.getType().isEmpty()) {
                r.setType(detectedType);
            } else {
                // Override to PC if name contains PC or A6
                if ("PC".equals(detectedType) && updated.getName() != null) {
                    String upperName = updated.getName().toUpperCase();
                    if (upperName.contains("PC") || upperName.contains("A6")) {
                        r.setType("PC");
                    } else {
                        r.setType(updated.getType());
                    }
                } else {
                    r.setType(updated.getType());
                }
            }
            
            return roomRepo.save(r);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteRoom(Long id) {
        if (roomRepo.existsById(id)) {
            roomRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public String importRoomsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int newCount = 0;
        int updatedCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            
            // 1. Find Header Row (Scan first 10 rows)
            Row headerRow = null;
            int nameColIdx = -1;
            int capColIdx = -1;
            int typeColIdx = -1;

            for (int r = 0; r <= 10; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                
                // Scan columns to find keywords
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String cellVal = getCellValue(row.getCell(c)).toLowerCase();
                    if (cellVal.contains("phòng học") || cellVal.contains("room name")) nameColIdx = c;
                    if (cellVal.contains("số sv") || cellVal.contains("capacity") || cellVal.contains("planned size")) capColIdx = c;
                    if (cellVal.contains("loại phòng") || cellVal.contains("room type")) typeColIdx = c;
                }
                
                // If found Name column, assume this is header
                if (nameColIdx != -1) {
                    headerRow = row;
                    break;
                }
            }

            // Fallback: If no header found, assume Standard Template (0: Name, 1: Cap, 2: Type)
            if (headerRow == null) {
                nameColIdx = 0;
                capColIdx = 1;
                typeColIdx = 2;
            }

            // 2. Iterate Data Rows
            int startRow = (headerRow != null) ? headerRow.getRowNum() + 1 : 1;
            
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String name = getCellValue(row.getCell(nameColIdx));
                    if (name.isEmpty()) continue;

                    // Parse Capacity
                    int capacity = 60; // Default
                    if (capColIdx != -1) {
                        try {
                            String capStr = getCellValue(row.getCell(capColIdx));
                            if (!capStr.isEmpty()) capacity = (int) Double.parseDouble(capStr);
                        } catch (Exception ignored) {}
                    }

                    // Parse/Infer Type
                    String type = "THEORY"; // Default
                    if (typeColIdx != -1) {
                        String typeStr = getCellValue(row.getCell(typeColIdx)).toUpperCase();
                        if (!typeStr.isEmpty()) type = typeStr;
                    } else {
                        // Auto-inference from Name if Type column missing
                        type = inferRoomType(name);
                    }

                    // 3. Save or Update
                    Optional<Room> existingRoom = roomRepo.findByName(name);
                    if (existingRoom.isPresent()) {
                        Room room = existingRoom.get();
                        boolean updated = false;
                        
                        // Update capacity if new one is larger (adapt to max class size)
                        if (capacity > room.getCapacity()) {
                            room.setCapacity(capacity);
                            updated = true;
                        }
                        
                        // Auto-update type based on name (PC/A6 -> LAB)
                        String detectedType = inferRoomType(name);
                        if (!room.getType().equals(detectedType)) {
                            room.setType(detectedType);
                            updated = true;
                        }
                        
                        if (updated) {
                            roomRepo.save(room);
                            updatedCount++;
                        }
                    } else {
                        Room room = new Room();
                        room.setName(name);
                        room.setCapacity(capacity);
                        room.setType(type);
                        roomRepo.save(room);
                        newCount++;
                    }

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + " Error: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "File Error: " + e.getMessage();
        }
        
        return String.format("Import completed! New Rooms: %d, Updated Capacity: %d. Errors: %d\n%s", 
                newCount, updatedCount, errors.size(), errors);
    }

    // Helper: Infer Room Type based on Name keywords
    // Automatically detects PC rooms as separate type from LAB
    private String inferRoomType(String name) {
        String n = name.toUpperCase();
        if (n.contains("ONLINE") || n.contains("TEAM") || n.contains("ZOOM") || n.contains("ELEARNING")) return "ONLINE";
        
        // PC rooms and A6 rooms are automatically detected as PC type (separate from LAB)
        // This includes: "PC", "A6-xxx (PC)", "A6-501", etc.
        if (n.contains("PC") || n.contains("A6")) return "PC";
        
        // LAB keywords (excluding PC)
        if (n.contains("(TH)") || n.contains("LAB") || n.contains("XƯỞNG") || n.contains("(TN)")) return "LAB";
        
        if (n.contains("SÂN") || n.contains("HỘI TRƯỜNG") || n.contains("NHÀ") || n.contains("HALL")) return "HALL";
        return "THEORY";
    }

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}