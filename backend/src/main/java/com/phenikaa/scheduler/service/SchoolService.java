package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.repository.SchoolRepository;
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
public class SchoolService {

    @Autowired private SchoolRepository schoolRepo;

    public List<School> getAllSchools() {
        return schoolRepo.findAll();
    }

    @SuppressWarnings("null")
    public java.util.Optional<School> getSchoolById(Long id) {
        return schoolRepo.findById(id);
    }

    @SuppressWarnings("null")
    public School createSchool(School school) {
        return schoolRepo.save(school);
    }

    @SuppressWarnings("null")
    public java.util.Optional<School> updateSchool(Long id, School updated) {
        return schoolRepo.findById(id).map(s -> {
            s.setCode(updated.getCode());
            s.setName(updated.getName());
            return schoolRepo.save(s);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteSchool(Long id) {
        if (schoolRepo.existsById(id)) {
            schoolRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public String importSchoolsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // Duyệt từ dòng 1 (bỏ Header)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Cột 0: Mã Trường | Cột 1: Tên Trường
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));

                    if (code.isEmpty() || name.isEmpty()) continue;

                    // Tìm hoặc tạo mới School
                    School school = schoolRepo.findByCode(code).orElse(new School());
                    school.setCode(code);
                    school.setName(name);

                    schoolRepo.save(school);
                    successCount++;

                } catch (Exception ex) {
                    errors.add("Lỗi dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "Lỗi đọc file: " + e.getMessage();
        }
        return "Import xong! Thành công: " + successCount + ". Lỗi: " + errors.size() + "\n" + errors;
    }

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}