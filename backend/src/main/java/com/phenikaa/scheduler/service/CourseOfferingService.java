package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.model.User;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.CourseRepository;
import com.phenikaa.scheduler.repository.LecturerRepository;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.security.SecurityUtils;
import com.phenikaa.scheduler.security.services.UserDetailsImpl;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseOfferingService {

    @Autowired
    private CourseOfferingRepository offeringRepo;
    @Autowired
    private CourseRepository courseRepo;
    @Autowired
    private LecturerRepository lecturerRepo;
    @Autowired
    private SemesterRepository semesterRepo;
    @Autowired 
    private SecurityUtils securityUtils;

    public List<CourseOffering> getAllOfferings() {
        UserDetailsImpl user = securityUtils.getCurrentUser();
        String role = user.getAuthorities().iterator().next().getAuthority();

        // 1. ADMIN (Đại học): Thấy hết để xếp lịch tổng
        if (role.equals("ADMIN")) return offeringRepo.findAll();

        // 2. ADMIN_SCHOOL: Thấy lớp của các khoa trực thuộc HOẶC lớp do trường quản lý trực tiếp
        if (role.equals("ADMIN_SCHOOL") && user.getSchoolId() != null) {
            return offeringRepo.findAllBySchoolId(user.getSchoolId());
        }

        // 3. ADMIN_FACULTY: Chỉ thấy lớp khoa mình tạo
        if (role.equals("ADMIN_FACULTY") && user.getFacultyId() != null) {
            return offeringRepo.findByCourse_ManagingFaculty_Id(user.getFacultyId());
        }

        return new ArrayList<>();
    }

    @SuppressWarnings("null")
    public java.util.Optional<CourseOffering> getOfferingById(Long id) {
        return offeringRepo.findById(id);
    }

    @SuppressWarnings("null")
    public CourseOffering createOffering(CourseOffering offering) {
        if (offering.getCourse() == null) {
            throw new RuntimeException("Course is required for CourseOffering");
        }

        // Resolve Course by id or courseCode
        if (offering.getCourse().getId() != null) {
            courseRepo.findById(offering.getCourse().getId()).ifPresent(offering::setCourse);
        } else if (offering.getCourse().getCourseCode() != null) {
            courseRepo.findByCourseCode(offering.getCourse().getCourseCode()).ifPresent(offering::setCourse);
        }

        // Resolve Semester: if not provided, use current active semester (if any)
        if (offering.getSemester() == null || offering.getSemester().getId() == null) {
            semesterRepo.findByIsCurrentTrue().ifPresent(offering::setSemester);
        } else {
            semesterRepo.findById(offering.getSemester().getId()).ifPresent(offering::setSemester);
        }

        // Resolve Parent offering if provided (by id or code)
        if (offering.getParent() != null) {
            if (offering.getParent().getId() != null) {
                offeringRepo.findById(offering.getParent().getId()).ifPresent(offering::setParent);
            } else if (offering.getParent().getCode() != null) {
                offeringRepo.findByCode(offering.getParent().getCode()).ifPresent(offering::setParent);
            }
        }

        if (offering.getClassType() == null || offering.getClassType().isEmpty()) {
            offering.setClassType("ALL");
        }

        if (offering.getStatus() == null || offering.getStatus().isEmpty()) {
            offering.setStatus("PLANNED");
        }

        return offeringRepo.save(offering);
    }

    @SuppressWarnings("null")
    public java.util.Optional<CourseOffering> updateOffering(Long id, CourseOffering updated) {
        return offeringRepo.findById(id).map(existing -> {
            existing.setCode(updated.getCode());
            existing.setPlannedSize(updated.getPlannedSize());
            existing.setTargetClasses(updated.getTargetClasses());

            if (updated.getClassType() != null && !updated.getClassType().isEmpty()) {
                existing.setClassType(updated.getClassType());
            }

            // Course resolution similar to create
            if (updated.getCourse() != null) {
                if (updated.getCourse().getId() != null) {
                    courseRepo.findById(updated.getCourse().getId()).ifPresent(existing::setCourse);
                } else if (updated.getCourse().getCourseCode() != null) {
                    courseRepo.findByCourseCode(updated.getCourse().getCourseCode()).ifPresent(existing::setCourse);
                }
            }

            // Semester update (optional)
            if (updated.getSemester() != null && updated.getSemester().getId() != null) {
                semesterRepo.findById(updated.getSemester().getId()).ifPresent(existing::setSemester);
            }

            // Parent update (optional)
            if (updated.getParent() != null) {
                if (updated.getParent().getId() != null) {
                    offeringRepo.findById(updated.getParent().getId()).ifPresent(existing::setParent);
                } else if (updated.getParent().getCode() != null) {
                    offeringRepo.findByCode(updated.getParent().getCode()).ifPresent(existing::setParent);
                }
            }

            return offeringRepo.save(existing);
        });
    }

    @SuppressWarnings("null")
    public boolean deleteOffering(Long id) {
        if (offeringRepo.existsById(id)) {
            offeringRepo.deleteById(id);
            return true;
        }
        return false;
    }

    public CourseOffering assignLecturer(Long offeringId, Long lecturerId) {
        @SuppressWarnings("null")
        CourseOffering offering = offeringRepo.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Offering not found"));
        
        if (lecturerId == null) {
            offering.setLecturer(null);
        } else {
            Lecturer lecturer = lecturerRepo.findById(lecturerId)
                    .orElseThrow(() -> new RuntimeException("Lecturer not found"));
            offering.setLecturer(lecturer);
        }
        
        return offeringRepo.save(offering);
    }

    @Transactional
    public String importCourseOfferingsExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int successCount = 0;

        // 1. Lấy Học kỳ hiện tại (Active)
        Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
        if (currentSem == null) {
            return "Error: No active semester found! Please activate a semester first.";
        }

        try (InputStream is = file.getInputStream();
                Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            // --- VÒNG 1: XỬ LÝ LỚP MẸ (LT, ALL, ELN) TRƯỚC ---
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String classType = getCellValue(row.getCell(5)); // Giả sử cột 5 là Type
                if ("TH".equalsIgnoreCase(classType))
                    continue; // Bỏ qua lớp TH ở vòng này

                processRow(row, i, errors, false, currentSem); // False = Không xử lý Parent
            }

            // --- VÒNG 2: XỬ LÝ LỚP CON (TH) ---
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                String classType = getCellValue(row.getCell(5));
                if (!"TH".equalsIgnoreCase(classType))
                    continue; // Chỉ xử lý lớp TH

                boolean created = processRow(row, i, errors, true, currentSem); // True = Xử lý Parent
                if (created)
                    successCount++;
            }

            // Đếm lại tổng số lớp LT/ALL đã tạo ở vòng 1 (để báo cáo cho đúng)
            successCount += (sheet.getLastRowNum() - errors.size() - successCount);

        } catch (Exception e) {
            return "File Error: " + e.getMessage();
        }
        return "Import completed! Errors: " + errors.size() + "\n" + errors;
    }

    private boolean processRow(Row row, int rowIndex, List<String> errors, boolean isChildPass, Semester semester) {
        try {
            // Đọc dữ liệu từ các cột
            // 0: Class Code | 1: Course Code | 2: Size | 3: Target Classes
            // 4: Lecturer | 5: Type (LT/TH/ALL) | 6: Parent Code (Nếu là TH)

            String classCode = getCellValue(row.getCell(0));
            String courseCode = getCellValue(row.getCell(1));
            String sizeStr = getCellValue(row.getCell(2));
            String targetClasses = getCellValue(row.getCell(3));
            String lecturerCode = getCellValue(row.getCell(4));
            String classType = getCellValue(row.getCell(5)).toUpperCase();
            String parentCode = getCellValue(row.getCell(6));

            if (classCode.isEmpty() || courseCode.isEmpty())
                return false;

            // Validate Course
            Optional<Course> courseOpt = courseRepo.findByCourseCode(courseCode);
            if (courseOpt.isEmpty()) {
                errors.add("Row " + (rowIndex + 1) + ": Course '" + courseCode + "' not found.");
                return false;
            }

            CourseOffering offering = offeringRepo.findByCode(classCode).orElse(new CourseOffering());
            offering.setCode(classCode);
            offering.setCourse(courseOpt.get());
            offering.setTargetClasses(targetClasses);
            offering.setClassType(classType.isEmpty() ? "ALL" : classType);
            offering.setStatus("PLANNED");
            offering.setSemester(semester); // Gán kỳ học hiện tại

            // Xử lý Size
            try {
                offering.setPlannedSize((int) Double.parseDouble(sizeStr));
            } catch (Exception e) {
                offering.setPlannedSize(60);
            }

            // Xử lý Giảng viên (nếu có)
            if (!lecturerCode.isEmpty()) {
                Optional<Lecturer> lecOpt = lecturerRepo.findByLecturerCode(lecturerCode);
                if (lecOpt.isPresent())
                    offering.setLecturer(lecOpt.get());
            }

            // --- QUY TẮC QUAN TRỌNG: LIÊN KẾT CHA CON ---
            if (isChildPass && "TH".equals(classType)) {
                if (parentCode.isEmpty()) {
                    errors.add("Row " + (rowIndex + 1) + ": Class Type is TH but Parent Code is empty.");
                    return false;
                }

                Optional<CourseOffering> parentOpt = offeringRepo.findByCode(parentCode);
                if (parentOpt.isEmpty()) {
                    errors.add("Row " + (rowIndex + 1) + ": Parent Class '" + parentCode
                            + "' not found (Make sure Parent is LT/ALL).");
                    return false;
                }
                offering.setParent(parentOpt.get());
            }

            offeringRepo.save(offering);
            return true;

        } catch (Exception ex) {
            errors.add("Row " + (rowIndex + 1) + " Error: " + ex.getMessage());
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private String getCellValue(Cell cell) {
        if (cell == null)
            return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

    public List<CourseOffering> getOfferingsForUser(User user) {
        List<CourseOffering> all = offeringRepo.findAll();

        if (user.getRole().equals("ADMIN_TRUONG")) {
            return all; // Thấy hết
        }

        if (user.getRole().equals("ADMIN_KHOA")) {
            Long myFacultyId = user.getFaculty().getId();

            return all.stream().filter(offering -> {
                // ĐK 1: Lớp này thuộc chuyên môn khoa tôi (Tôi là Provider)
                boolean isMyExpertise = offering.getCourse().getManagingFaculty() != null
                        && offering.getCourse().getManagingFaculty().getId().equals(myFacultyId);

                // ĐK 2: Lớp này dành cho sinh viên khoa tôi (Tôi là Owner)
                // (Giả sử ta check targetClasses có chứa mã khoa, hoặc logic mapping khác)
                boolean isForMyStudents = offering.getTargetClasses().contains(user.getFaculty().getCode()); // VD:
                                                                                                             // K17-CNTT

                return isMyExpertise || isForMyStudents;
            }).collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}