package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Curriculum;
import com.phenikaa.scheduler.model.CurriculumDetail;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.CurriculumDetailRepository;
import com.phenikaa.scheduler.repository.CurriculumRepository;
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
public class CurriculumDetailService {

    @Autowired private CurriculumDetailRepository detailRepo;
    @Autowired private CurriculumRepository curriculumRepo;
    @Autowired private CourseRepository courseRepo;

    public List<CurriculumDetail> getAllDetails() {
        return detailRepo.findAll();
    }

    public String importDetailsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    // Col 0: Curriculum Name | Col 1: Course Code | Col 2: Semester Index (int)
                    String currName = getCellValue(row.getCell(0));
                    String courseCode = getCellValue(row.getCell(1));
                    String semesterStr = getCellValue(row.getCell(2));

                    if (currName.isEmpty() || courseCode.isEmpty()) continue;

                    // 1. Validate Parents
                    Optional<Curriculum> currOpt = curriculumRepo.findByName(currName);
                    if (currOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Curriculum '" + currName + "' not found.");
                        continue;
                    }

                    Optional<Course> courseOpt = courseRepo.findByCourseCode(courseCode);
                    if (courseOpt.isEmpty()) {
                        errors.add("Row " + (i + 1) + ": Course '" + courseCode + "' not found.");
                        continue;
                    }

                    // 2. Create/Update Detail
                    CurriculumDetail detail = detailRepo.findByCurriculumAndCourse(currOpt.get(), courseOpt.get())
                            .orElse(new CurriculumDetail());
                    
                    detail.setCurriculum(currOpt.get());
                    detail.setCourse(courseOpt.get());
                    
                    // Parse Semester Index
                    // Support multiple semesters like "1,2" or "1"
                    detail.setSemesterIndex(semesterStr);
                    
                    // Lấy số tín chỉ từ môn học để lưu (nếu cần hiển thị nhanh, nhưng trong entity mình chỉ map semester)
                    // detail.setCredits(...) // Nếu entity có trường này

                    detailRepo.save(detail);
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