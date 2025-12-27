package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.Major;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.MajorRepository;
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
public class MajorService {

    @Autowired private MajorRepository majorRepo;
    @Autowired private FacultyRepository facultyRepo;

    public List<Major> getAllMajors() {
        return majorRepo.findAll();
    }

    public List<Major> getMajorsBySchoolId(Long schoolId) {
        return majorRepo.findByFacultySchoolId(schoolId);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Major> getMajorById(Long id) {
        return majorRepo.findById(id);
    }

    @SuppressWarnings("null")
    public Major createMajor(Major major) {
        if (major.getFaculty() != null) {
            if (major.getFaculty().getId() != null) {
                facultyRepo.findById(major.getFaculty().getId()).ifPresent(major::setFaculty);
            } else if (major.getFaculty().getCode() != null) {
                facultyRepo.findByCode(major.getFaculty().getCode()).ifPresent(major::setFaculty);
            }
        }
        return majorRepo.save(major);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Major> updateMajor(Long id, Major updated) {
        return majorRepo.findById(id).map(m -> {
            m.setCode(updated.getCode());
            m.setName(updated.getName());
            if (updated.getFaculty() != null) {
                if (updated.getFaculty().getId() != null) {
                    facultyRepo.findById(updated.getFaculty().getId()).ifPresent(m::setFaculty);
                } else if (updated.getFaculty().getCode() != null) {
                    facultyRepo.findByCode(updated.getFaculty().getCode()).ifPresent(m::setFaculty);
                }
            }
            return majorRepo.save(m);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteMajor(Long id) {
        if (majorRepo.existsById(id)) {
            majorRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public String importMajorsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Start from row 1 (skip header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Major Code | Col 1: Major Name | Col 2: Faculty Code
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String facultyCode = getCellValue(row.getCell(2));

                    if (code.isEmpty() || name.isEmpty()) continue;

                    // 1. Check Faculty existence
                    Optional<Faculty> facultyOpt = facultyRepo.findByCode(facultyCode);
                    if (facultyOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Faculty Code '" + facultyCode + "' not found.");
                        continue;
                    }

                    // 2. Create or Update Major
                    Major major = majorRepo.findByCode(code).orElse(new Major());
                    major.setCode(code);
                    major.setName(name);
                    major.setFaculty(facultyOpt.get());

                    majorRepo.save(major);
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

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}