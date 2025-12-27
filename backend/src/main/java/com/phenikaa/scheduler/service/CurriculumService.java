package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Cohort;
import com.phenikaa.scheduler.model.Curriculum;
import com.phenikaa.scheduler.model.Major;
import com.phenikaa.scheduler.repository.CohortRepository;
import com.phenikaa.scheduler.repository.CurriculumRepository;
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
public class CurriculumService {

    @Autowired private CurriculumRepository curriculumRepo;
    @Autowired private MajorRepository majorRepo;
    @Autowired private CohortRepository cohortRepo;

    public List<Curriculum> getAllCurricula() {
        return curriculumRepo.findAll();
    }

    public String importCurriculaExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Curriculum Name | Col 1: Major Code | Col 2: Cohort Name
                    String name = getCellValue(row.getCell(0));
                    String majorCode = getCellValue(row.getCell(1));
                    String cohortName = getCellValue(row.getCell(2));

                    if (name.isEmpty()) continue;

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

                    // 2. Create/Update Curriculum
                    Curriculum curriculum = curriculumRepo.findByName(name).orElse(new Curriculum());
                    curriculum.setName(name);
                    curriculum.setMajor(majorOpt.get());
                    curriculum.setCohort(cohortOpt.get());

                    curriculumRepo.save(curriculum);
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