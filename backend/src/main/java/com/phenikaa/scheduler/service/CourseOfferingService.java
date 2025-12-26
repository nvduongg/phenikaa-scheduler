package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.LecturerRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CourseOfferingService {

    @Autowired private CourseOfferingRepository offeringRepo;
    @Autowired private CourseRepository courseRepo;
    @Autowired private LecturerRepository lecturerRepo;

    public List<CourseOffering> getAllOfferings() {
        return offeringRepo.findAll();
    }

    public String importCourseOfferingsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Class Code (Mã lớp HP) | Col 1: Course Code | Col 2: Size | Col 3: Target Classes | Col 4: Lecturer Code (Optional)
                    String classCode = getCellValue(row.getCell(0));
                    String courseCode = getCellValue(row.getCell(1));
                    String sizeStr = getCellValue(row.getCell(2));
                    String targetClasses = getCellValue(row.getCell(3));
                    String lecturerCode = getCellValue(row.getCell(4)); // Cột mới: Giảng viên cố định (nếu có)

                    if (classCode.isEmpty() || courseCode.isEmpty()) continue;

                    // 1. Validate Course
                    Optional<Course> courseOpt = courseRepo.findByCourseCode(courseCode);
                    if (courseOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Course '" + courseCode + "' not found.");
                        continue;
                    }

                    // 2. Create Offering
                    CourseOffering offering = offeringRepo.findByCode(classCode).orElse(new CourseOffering());
                    offering.setCode(classCode);
                    offering.setCourse(courseOpt.get());
                    offering.setTargetClasses(targetClasses);
                    offering.setStatus("PLANNED"); // Reset status về chưa xếp

                    try {
                        offering.setPlannedSize((int) Double.parseDouble(sizeStr));
                    } catch (Exception e) {
                        offering.setPlannedSize(60);
                    }

                    // 3. Handle Fixed Lecturer (Optional)
                    if (!lecturerCode.isEmpty()) {
                        Optional<Lecturer> lecOpt = lecturerRepo.findByLecturerCode(lecturerCode);
                        if (lecOpt.isPresent()) {
                            offering.setLecturer(lecOpt.get());
                        } else {
                            errors.add("Row " + (i + 1) + ": Lecturer '" + lecturerCode + "' not found (skipped assignment).");
                        }
                    } else {
                        offering.setLecturer(null); // Để trống cho thuật toán xếp
                    }
                    
                    // Reset kết quả xếp lịch cũ (nếu import đè)
                    offering.setRoom(null);
                    offering.setDayOfWeek(null);
                    offering.setStartPeriod(null);
                    offering.setEndPeriod(null);

                    offeringRepo.save(offering);
                    successCount++;

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + " Error: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "File Error: " + e.getMessage();
        }
        return "Import completed! Success: " + successCount + ". Errors: " + errors.size() + "\n" + errors;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}