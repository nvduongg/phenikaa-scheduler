package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Lecturer;
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

        // 4. Bắt đầu thuật toán Greedy
        for (CourseOffering offering : unassignedOfferings) {
            
            // Bước 4a: Tìm các ứng viên (Candidates) dạy được môn này
            // Logic: Giảng viên phải có môn này trong list teachingCourses
            List<Lecturer> candidates = allLecturers.stream()
                .filter(l -> l.getTeachingCourses().contains(offering.getCourse()))
                .collect(Collectors.toList());

            if (candidates.isEmpty()) {
                skippedCount++; // Không ai dạy được môn này
                continue;
            }

            // Bước 4b: Sắp xếp ứng viên theo tiêu chí "Ai rảnh việc nhất thì giao" (Load Balancing)
            candidates.sort(Comparator.comparingInt(l -> lecturerLoadMap.get(l.getId())));

            // Bước 4c: Chọn người đầu tiên (người ít việc nhất)
            Lecturer chosenOne = candidates.get(0);

            // Bước 4d: Gán và cập nhật
            offering.setLecturer(chosenOne);
            offeringRepo.save(offering);

            // Tăng workload của giảng viên này lên 1
            lecturerLoadMap.put(chosenOne.getId(), lecturerLoadMap.get(chosenOne.getId()) + 1);
            
            assignedCount++;
        }

        return String.format("Auto-assign completed. Assigned: %d classes. Skipped (No expertise found): %d classes.", 
                assignedCount, skippedCount);
    }
}