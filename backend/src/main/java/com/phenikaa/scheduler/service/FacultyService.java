package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.repository.FacultyRepository;
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
import java.util.Optional;

@Service
public class FacultyService {

    @Autowired private FacultyRepository facultyRepo;
    @Autowired private SchoolRepository schoolRepo;

    public List<Faculty> getAllFaculties() {
        return facultyRepo.findAll();
    }

    public String importFacultiesExcel(MultipartFile file) {
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
                    // Cột 0: Mã Khoa | Cột 1: Tên Khoa | Cột 2: Mã Trường (School Code - Optional)
                    String code = getCellValue(row.getCell(0));
                    String name = getCellValue(row.getCell(1));
                    String schoolCode = getCellValue(row.getCell(2));

                    if (code.isEmpty() || name.isEmpty()) continue;

                    // 1. Tìm hoặc tạo mới Faculty
                    Faculty faculty = facultyRepo.findByCode(code).orElse(new Faculty());
                    faculty.setCode(code);
                    faculty.setName(name);

                    // 2. Xử lý liên kết School (nếu có nhập)
                    if (!schoolCode.isEmpty()) {
                        Optional<School> schoolOpt = schoolRepo.findByCode(schoolCode);
                        if (schoolOpt.isPresent()) {
                            faculty.setSchool(schoolOpt.get());
                        } else {
                            errors.add("Dòng " + (i + 1) + ": Không tìm thấy Mã trường '" + schoolCode + "'");
                            // Vẫn lưu Khoa nhưng không gán Trường (tùy nghiệp vụ)
                        }
                    } else {
                        faculty.setSchool(null); // Trực thuộc Đại học
                    }

                    facultyRepo.save(faculty);
                    successCount++;

                } catch (Exception ex) {
                    errors.add("Lỗi dòng " + (i + 1) + ": " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            return "Lỗi đọc file: " + e.getMessage();
        }
        return "Import hoàn tất! Thành công: " + successCount + ". Lỗi: " + errors.size() + "\n" + errors;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}