package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.model.School;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.LecturerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AutoAssignService {

    @Autowired private CourseOfferingRepository offeringRepo;
    @Autowired private LecturerRepository lecturerRepo;

    @Transactional
    public String autoAssignLecturers() {
        // 1. Lấy tất cả lớp chưa có giảng viên (hoặc lấy hết để xếp lại từ đầu nếu muốn)
        // Ở đây ta lấy các lớp chưa có Lecturer
        List<CourseOffering> unassignedOfferings = offeringRepo.findAll().stream()
                .filter(offering -> offering.getLecturer() == null)
                .collect(Collectors.toList());

        if (unassignedOfferings.isEmpty()) {
            return "No unassigned classes found.";
        }

        // 2. Lấy danh sách toàn bộ giảng viên kèm thông tin chuyên môn
        List<Lecturer> allLecturers = lecturerRepo.findAll();

        // 3. Tạo Map để theo dõi "Workload" (Số lớp đã nhận) của từng giảng viên trong phiên chạy này
        // Key: LecturerID, Value: Số lớp đã gán
        Map<Long, Integer> lecturerLoadMap = new HashMap<>();
        for (Lecturer l : allLecturers) {
            // Đếm số lớp họ đã có sẵn trong DB (để cộng dồn)
            long currentLoad = offeringRepo.countByLecturerId(l.getId());
            lecturerLoadMap.put(l.getId(), (int) currentLoad);
        }

        int assignedCount = 0;
        int skippedCount = 0;
        int assignedByExpertise = 0;
        int assignedByFaculty = 0;
        int assignedBySchool = 0;

        // 4. Bắt đầu thuật toán Greedy
        for (CourseOffering offering : unassignedOfferings) {
            
            Course course = offering.getCourse();
            Long managingFacultyId = (course != null && course.getManagingFaculty() != null)
                    ? course.getManagingFaculty().getId()
                    : null;
            School managingSchool = resolveManagingSchool(course);

            // Bước 4a: Tìm các ứng viên (Candidates)
            // 1) Ưu tiên GV có chuyên môn dạy được môn (teachingCourses)
            // 2) Nếu môn do khoa quản lý: GV cùng khoa có thể được phân
            // 3) Nếu môn do trường thành viên quản lý: GV thuộc trường đó có thể được phân
            List<Lecturer> candidates = allLecturers.stream()
                .filter(l -> {
                    if (l == null || l.getFaculty() == null) return false;
                    boolean expertiseMatch = canTeachCourse(l, course);
                    boolean facultyMatch = managingFacultyId != null && l.getFaculty().getId().equals(managingFacultyId);
                    boolean schoolMatch = managingSchool != null
                            && l.getFaculty().getSchool() != null
                            && managingSchool.getId() != null
                            && managingSchool.getId().equals(l.getFaculty().getSchool().getId());
                    return expertiseMatch || facultyMatch || schoolMatch;
                })
                .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                skippedCount++; // Không ai dạy được môn này
                continue;
            }

            // Bước 4b: Sắp xếp ứng viên theo tiêu chí "Ai rảnh việc nhất thì giao" (Load Balancing)
            candidates.sort(
                Comparator
                    // Ưu tiên chuyên môn trước
                    .comparing((Lecturer l) -> !canTeachCourse(l, course))
                    // Sau đó ưu tiên cùng khoa quản lý môn
                    .thenComparing(l -> !(managingFacultyId != null && l.getFaculty() != null && managingFacultyId.equals(l.getFaculty().getId())))
                    // Sau đó ưu tiên cùng trường quản lý môn
                    .thenComparing(l -> !(managingSchool != null
                            && l.getFaculty() != null
                            && l.getFaculty().getSchool() != null
                            && managingSchool.getId() != null
                            && managingSchool.getId().equals(l.getFaculty().getSchool().getId())))
                    // Cuối cùng cân bằng tải
                    .thenComparingInt(l -> lecturerLoadMap.getOrDefault(l.getId(), 0))
            );

            // Bước 4c: Chọn người đầu tiên (người ít việc nhất)
            Lecturer chosenOne = candidates.get(0);

            // Bước 4d: Gán và cập nhật
            offering.setLecturer(chosenOne);
            offeringRepo.save(offering);

                // Thống kê kiểu gán
                boolean isExpertise = canTeachCourse(chosenOne, course);
                boolean isFaculty = managingFacultyId != null
                    && chosenOne.getFaculty() != null
                    && managingFacultyId.equals(chosenOne.getFaculty().getId());
                boolean isSchool = managingSchool != null
                    && chosenOne.getFaculty() != null
                    && chosenOne.getFaculty().getSchool() != null
                    && managingSchool.getId() != null
                    && managingSchool.getId().equals(chosenOne.getFaculty().getSchool().getId());
                if (isExpertise) assignedByExpertise++;
                else if (isFaculty) assignedByFaculty++;
                else if (isSchool) assignedBySchool++;

            // Tăng workload của giảng viên này lên 1
            lecturerLoadMap.put(chosenOne.getId(), lecturerLoadMap.get(chosenOne.getId()) + 1);
            
            assignedCount++;
        }

        return String.format(
                "Auto-assign completed. Assigned: %d classes (Expertise: %d, Faculty: %d, School: %d). Skipped: %d classes.",
                assignedCount, assignedByExpertise, assignedByFaculty, assignedBySchool, skippedCount
        );
    }

    private boolean canTeachCourse(Lecturer lecturer, Course course) {
        if (lecturer == null || course == null) return false;
        if (course.getId() == null) {
            // Fallback: giữ hành vi cũ nếu Course chưa có id
            return lecturer.getTeachingCourses() != null && lecturer.getTeachingCourses().contains(course);
        }
        if (lecturer.getTeachingCourses() == null) return false;
        return lecturer.getTeachingCourses().stream()
                .filter(Objects::nonNull)
                .anyMatch(c -> c.getId() != null && c.getId().equals(course.getId()));
    }

    private School resolveManagingSchool(Course course) {
        if (course == null) return null;
        // Ưu tiên school gán trực tiếp cho Course
        if (course.getSchool() != null) return course.getSchool();
        // Fallback: school của managingFaculty
        if (course.getManagingFaculty() != null) return course.getManagingFaculty().getSchool();
        return null;
    }
}