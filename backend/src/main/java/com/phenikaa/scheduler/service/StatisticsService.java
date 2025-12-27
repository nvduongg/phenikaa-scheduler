package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.dto.LecturerWorkloadStat;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Lecturer;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired private CourseOfferingRepository offeringRepo;

    public List<LecturerWorkloadStat> getLecturerWorkload(Long semesterId) {
        // 1. Lấy danh sách lớp trong kỳ đã được xếp lịch và ĐÃ CÓ giảng viên
        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(semesterId).stream()
                .filter(o -> o.getLecturer() != null && "SCHEDULED".equals(o.getStatus()))
                .collect(Collectors.toList());

        // 2. Nhóm theo Giảng viên
        Map<Lecturer, List<CourseOffering>> groupedByLecturer = offerings.stream()
                .collect(Collectors.groupingBy(CourseOffering::getLecturer));

        List<LecturerWorkloadStat> stats = new ArrayList<>();

        // 3. Tính toán từng người
        for (Map.Entry<Lecturer, List<CourseOffering>> entry : groupedByLecturer.entrySet()) {
            Lecturer lec = entry.getKey();
            List<CourseOffering> classes = entry.getValue();

            long totalTheory = 0;
            long totalPractice = 0;

            for (CourseOffering c : classes) {
                // Tính số tiết mỗi buổi: End - Start + 1
                // Nếu chưa xếp lịch (Start=null) thì lấy duration dự kiến
                int durationPerSession;
                if (c.getStartPeriod() != null && c.getEndPeriod() != null) {
                    durationPerSession = c.getEndPeriod() - c.getStartPeriod() + 1;
                } else {
                    // Fallback nếu dữ liệu lỗi
                    durationPerSession = (int) Math.ceil(c.getCourse().getCredits());
                }

                // Tạm thời: dùng số tuần mặc định (ví dụ 15 tuần)
                long weeks = 15;

                long totalForClass = durationPerSession * weeks;

                // Cộng dồn
                String type = c.getClassType() != null ? c.getClassType() : "LT";
                if ("TH".equalsIgnoreCase(type)) {
                    totalPractice += totalForClass;
                } else {
                    totalTheory += totalForClass;
                }
            }

            stats.add(new LecturerWorkloadStat(
                    lec.getId(),
                    lec.getFullName(),
                    lec.getEmail(),
                    classes.size(),
                    totalTheory,
                    totalPractice,
                    totalTheory + totalPractice
            ));
        }
        
        // Sắp xếp theo tổng tiết giảm dần (Người dạy nhiều nhất lên đầu)
        stats.sort((a, b) -> Long.compare(b.getTotalPeriod(), a.getTotalPeriod()));

        return stats;
    }
}