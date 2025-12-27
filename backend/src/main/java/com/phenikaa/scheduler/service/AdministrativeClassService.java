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

    public List<AdministrativeClass> getClassesBySchoolId(Long schoolId) {
        return adminClassRepo.findByMajorFacultySchoolId(schoolId);
    }

    @SuppressWarnings("null")
    public java.util.Optional<AdministrativeClass> getClassById(Long id) {
        return adminClassRepo.findById(id);
    }

    @SuppressWarnings("null")
    public AdministrativeClass createClass(AdministrativeClass adminClass) {
        // Resolve Major and Cohort associations if IDs provided
        if (adminClass.getMajor() != null && adminClass.getMajor().getId() != null) {
            majorRepo.findById(adminClass.getMajor().getId()).ifPresent(adminClass::setMajor);
        }
        if (adminClass.getCohort() != null && adminClass.getCohort().getId() != null) {
            cohortRepo.findById(adminClass.getCohort().getId()).ifPresent(adminClass::setCohort);
        }
        return adminClassRepo.save(adminClass);
    }

    @SuppressWarnings("null")
    public java.util.Optional<AdministrativeClass> updateClass(Long id, AdministrativeClass updated) {
        return adminClassRepo.findById(id).map(c -> {
            c.setName(updated.getName());
            c.setCode(updated.getCode());
            if (updated.getMajor() != null) {
                if (updated.getMajor().getId() != null) majorRepo.findById(updated.getMajor().getId()).ifPresent(c::setMajor);
                else if (updated.getMajor().getCode() != null) majorRepo.findByCode(updated.getMajor().getCode()).ifPresent(c::setMajor);
            }
            if (updated.getCohort() != null) {
                if (updated.getCohort().getId() != null) cohortRepo.findById(updated.getCohort().getId()).ifPresent(c::setCohort);
                else if (updated.getCohort().getName() != null) cohortRepo.findByName(updated.getCohort().getName()).ifPresent(c::setCohort);
            }
            c.setSize(updated.getSize());
            return adminClassRepo.save(c);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteClass(Long id) {
        if (adminClassRepo.existsById(id)) {
            adminClassRepo.deleteById(id);
            return true;
        }
        return false;
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

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}