package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Lecturer;
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
public class ExpertiseService {

    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private CourseRepository courseRepo;

    public String importExpertiseExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Lecturer Code | Col 1: Course Code
                    String lecCode = getCellValue(row.getCell(0));
                    String courseCode = getCellValue(row.getCell(1));

                    if (lecCode.isEmpty() || courseCode.isEmpty()) continue;

                    // 1. Find Lecturer & Course
                    Optional<Lecturer> lecOpt = lecturerRepo.findByLecturerCode(lecCode);
                    Optional<Course> courseOpt = courseRepo.findByCourseCode(courseCode);

                    if (lecOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Lecturer '" + lecCode + "' not found");
                        continue;
                    }
                    if (courseOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Course '" + courseCode + "' not found");
                        continue;
                    }

                    Lecturer lecturer = lecOpt.get();
                    Course course = courseOpt.get();

                    // 2. Add course to lecturer if not exists
                    if (!lecturer.getTeachingCourses().contains(course)) {
                        lecturer.getTeachingCourses().add(course);
                        lecturerRepo.save(lecturer); // Save updated relationship
                        successCount++;
                    }

                } catch (Exception ex) {
                    errors.add("Row " + (i + 1) + " Error: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "File Error: " + e.getMessage();
        }
        return "Import completed! Mapped: " + successCount + ". Errors: " + errors.size() + "\n" + errors;
    }

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}