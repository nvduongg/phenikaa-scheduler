package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Cohort;
import com.phenikaa.scheduler.repository.CohortRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class CohortService {

    @Autowired private CohortRepository cohortRepo;

    public List<Cohort> getAllCohorts() {
        return cohortRepo.findAll();
    }

    public String importCohortsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Cohort Name | Col 1: Start Year | Col 2: End Year
                    String name = getCellValue(row.getCell(0));
                    String startYearStr = getCellValue(row.getCell(1));
                    String endYearStr = getCellValue(row.getCell(2));

                    if (name.isEmpty()) continue;

                    Cohort cohort = cohortRepo.findByName(name).orElse(new Cohort());
                    cohort.setName(name);
                    
                    try {
                        if (!startYearStr.isEmpty()) cohort.setStartYear((int) Double.parseDouble(startYearStr));
                        if (!endYearStr.isEmpty()) cohort.setEndYear((int) Double.parseDouble(endYearStr));
                    } catch (NumberFormatException e) {
                        // Ignore year parsing error, keep null or default
                    }

                    cohortRepo.save(cohort);
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