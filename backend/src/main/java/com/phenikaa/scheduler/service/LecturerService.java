package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.repository.FacultyRepository;
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
public class LecturerService {

    @Autowired private LecturerRepository lecturerRepo;
    @Autowired private FacultyRepository facultyRepo;

    public List<Lecturer> getAllLecturers() {
        return lecturerRepo.findAll();
    }

    public String importLecturersExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Lecturer Code | Col 1: Full Name | Col 2: Email | Col 3: Faculty Code
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String email = getCellValue(row.getCell(2));
                    String facultyCode = getCellValue(row.getCell(3));

                    if (code.isEmpty() || name.isEmpty()) continue;

                    // 1. Validate Faculty
                    Optional<Faculty> facultyOpt = facultyRepo.findByCode(facultyCode);
                    if (facultyOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Faculty Code '" + facultyCode + "' not found.");
                        continue;
                    }

                    // 2. Create/Update Lecturer
                    Lecturer lecturer = lecturerRepo.findByLecturerCode(code).orElse(new Lecturer());
                    lecturer.setLecturerCode(code);
                    lecturer.setFullName(name);
                    lecturer.setEmail(email);
                    lecturer.setFaculty(facultyOpt.get());

                    lecturerRepo.save(lecturer);
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