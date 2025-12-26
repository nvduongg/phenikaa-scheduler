package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.AdministrativeClass;
import com.phenikaa.scheduler.model.Cohort;
import com.phenikaa.scheduler.model.Major;
import com.phenikaa.scheduler.repository.AdministrativeClassRepository;
import com.phenikaa.scheduler.repository.CohortRepository;
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
public class AdministrativeClassService {

    @Autowired private AdministrativeClassRepository adminClassRepo;
    @Autowired private MajorRepository majorRepo;
    @Autowired private CohortRepository cohortRepo;

    public List<AdministrativeClass> getAllClasses() {
        return adminClassRepo.findAll();
    }

    public String importClassesExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Class Name | Col 1: Major Code | Col 2: Cohort Name | Col 3: Size
                    String className = getCellValue(row.getCell(0));
                    String majorCode = getCellValue(row.getCell(1));
                    String cohortName = getCellValue(row.getCell(2));
                    String sizeStr = getCellValue(row.getCell(3));

                    if (className.isEmpty()) continue;

                    // 1. Validate Major & Cohort
                    Optional<Major> majorOpt = majorRepo.findByCode(majorCode);
                    if (majorOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Major '" + majorCode + "' not found.");
                        continue;
                    }

                    Optional<Cohort> cohortOpt = cohortRepo.findByName(cohortName);
                    if (cohortOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Cohort '" + cohortName + "' not found.");
                        continue;
                    }

                    // 2. Create/Update Class
                    AdministrativeClass adminClass = adminClassRepo.findByName(className).orElse(new AdministrativeClass());
                    adminClass.setName(className);
                    adminClass.setMajor(majorOpt.get());
                    adminClass.setCohort(cohortOpt.get());
                    
                    try {
                        adminClass.setSize((int) Double.parseDouble(sizeStr));
                    } catch (NumberFormatException e) {
                        adminClass.setSize(0); // Default
                    }

                    adminClassRepo.save(adminClass);
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