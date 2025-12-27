package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Faculty;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.repository.FacultyRepository;
import com.phenikaa.scheduler.repository.SchoolRepository;
import com.phenikaa.scheduler.security.SecurityUtils;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class FacultyService {

    @Autowired private FacultyRepository facultyRepo;
    @Autowired private SchoolRepository schoolRepo;
    @Autowired private SecurityUtils securityUtils; // Nhớ inject cái này

    @SuppressWarnings("null")
    public List<Faculty> getAllFaculties() {
        UserDetailsImpl currentUser = securityUtils.getCurrentUser();
        
        // Nếu chưa đăng nhập (hoặc lỗi), trả về rỗng
        if (currentUser == null) return new ArrayList<>();

        String role = currentUser.getAuthorities().iterator().next().getAuthority();

        // 1. ADMIN ĐẠI HỌC: Thấy toàn bộ khoa trong ĐH Phenikaa
        if (role.equals("ADMIN")) {
            return facultyRepo.findAll();
        }

        // 2. ADMIN TRƯỜNG THÀNH VIÊN: Chỉ thấy các khoa con thuộc trường mình
        // Ví dụ: admin_psc chỉ thấy PSC1, PSC2, PSC3
        if (role.equals("ADMIN_SCHOOL") && currentUser.getSchoolId() != null) {
            return facultyRepo.findBySchoolId(currentUser.getSchoolId());
        }

        // 3. ADMIN KHOA: Chỉ thấy chính khoa mình
        if (role.equals("ADMIN_FACULTY") && currentUser.getFacultyId() != null) {
            // Trả về list chứa đúng 1 phần tử là khoa của họ
            return facultyRepo.findById(currentUser.getFacultyId())
                    .map(Collections::singletonList)
                    .orElse(new ArrayList<>());
        }

        return new ArrayList<>();
    }

    public List<Faculty> getFacultiesBySchoolId(Long schoolId) {
        return facultyRepo.findBySchoolId(schoolId);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Faculty> getFacultyById(Long id) {
        return facultyRepo.findById(id);
    }

    @SuppressWarnings("null")
    public Faculty createFaculty(Faculty faculty) {
        // attach school if provided
        if (faculty.getSchool() != null) {
            if (faculty.getSchool().getId() != null) {
                schoolRepo.findById(faculty.getSchool().getId()).ifPresent(faculty::setSchool);
            } else if (faculty.getSchool().getCode() != null) {
                schoolRepo.findByCode(faculty.getSchool().getCode()).ifPresent(faculty::setSchool);
            }
        }
        return facultyRepo.save(faculty);
    }

    @SuppressWarnings("null")
    public java.util.Optional<Faculty> updateFaculty(Long id, Faculty updated) {
        return facultyRepo.findById(id).map(f -> {
            f.setCode(updated.getCode());
            f.setName(updated.getName());
            if (updated.getSchool() != null) {
                if (updated.getSchool().getId() != null) {
                    schoolRepo.findById(updated.getSchool().getId()).ifPresent(f::setSchool);
                } else if (updated.getSchool().getCode() != null) {
                    schoolRepo.findByCode(updated.getSchool().getCode()).ifPresent(f::setSchool);
                }
            } else {
                f.setSchool(null);
            }
            return facultyRepo.save(f);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteFaculty(Long id) {
        if (facultyRepo.existsById(id)) {
            facultyRepo.deleteById(id);
            return true;
        }
        return false;
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

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }
}