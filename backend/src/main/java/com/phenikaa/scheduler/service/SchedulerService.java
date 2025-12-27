package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.core.GeneticAlgorithm;
import com.phenikaa.scheduler.validator.*;
import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Room;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.RoomRepository;
import com.phenikaa.scheduler.repository.SemesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SchedulerService {

    @Autowired private CourseOfferingRepository offeringRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private SemesterRepository semesterRepo;
    @Autowired private TimeTableValidator validator;
    @Autowired private GeneticAlgorithm geneticAlgorithm;

    @Transactional
    public String generateSchedule(String algorithm) {
        // Mặc định: GA nếu không truyền hoặc truyền GA
        if (algorithm == null || algorithm.equalsIgnoreCase("GA")) {
            Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (currentSem == null) return "Error: No active semester found! Please activate a semester first.";
            return geneticAlgorithm.runGeneticAlgorithm(currentSem.getId());
        }

        // Ngược lại dùng heuristic cũ
        return generateSchedule();
    }

    @Transactional
    public String generateSchedule() {
        // 1. Lấy Học kỳ hiện tại (Active Semester)
        Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
        if (currentSem == null) return "Error: No active semester found! Please activate a semester first.";

        // 2. Lấy danh sách lớp CỦA KỲ NÀY
        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(currentSem.getId());
        if (offerings.isEmpty()) return "Error: No offerings found for the current semester (" + currentSem.getName() + ")";

        List<Room> rooms = roomRepo.findAll();

        // 3. Reset trạng thái trước khi xếp (Clear lịch cũ của kỳ này)
        for (CourseOffering o : offerings) {
            o.setStatus("PLANNED");
            o.setStatusMessage(null);
            o.setDayOfWeek(null);
            o.setStartPeriod(null);
            o.setEndPeriod(null);
            o.setRoom(null);
        }

        // 4. Sắp xếp danh sách ưu tiên (Heuristic Strategy)
        // Ưu tiên 1: Lớp LT (Cha) xếp trước (Vì lớp to khó xếp hơn & để làm mốc cho lớp con)
        // Ưu tiên 2: Sĩ số giảm dần (Phòng to hiếm hơn phòng nhỏ)
        offerings.sort((o1, o2) -> {
            String type1 = o1.getClassType() == null ? "" : o1.getClassType();
            String type2 = o2.getClassType() == null ? "" : o2.getClassType();

            int score1 = type1.equals("LT") ? 2 : (type1.equals("TH") ? 1 : 0);
            int score2 = type2.equals("LT") ? 2 : (type2.equals("TH") ? 1 : 0);
            
            if (score1 != score2) return score2 - score1; // LT xếp trước TH
            return o2.getPlannedSize() - o1.getPlannedSize(); // Lớp đông xếp trước
        });

        // Sort phòng: Bé trước -> Lớn sau (Best Fit Strategy)
        // Để tránh lãng phí phòng to cho lớp nhỏ
        rooms.sort(Comparator.comparingInt(Room::getCapacity));

        List<CourseOffering> scheduledList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        // 5. VÒNG LẶP XẾP LỊCH CHÍNH
        for (CourseOffering offering : offerings) {
            Course course = offering.getCourse();
            String classType = offering.getClassType() != null ? offering.getClassType() : "ALL";

            // A. Xác định Loại phòng cần thiết
            // Mặc định là phòng học lý thuyết
            String requiredRoomType = "THEORY"; 
            
            if ("TH".equals(classType)) {
                requiredRoomType = "LAB";
            } else if ("ELN".equals(classType) || Boolean.TRUE.equals(course.getIsOnline())) {
                requiredRoomType = "ONLINE";
            }

            // B. Xác định Thời lượng (Số tiết) - Logic đồng bộ với Validator
            int duration = 3; // Mặc định
            if ("LT".equals(classType)) duration = (int) Math.ceil(course.getTheoryCredits());
            else if ("TH".equals(classType)) duration = (int) Math.ceil(course.getPracticeCredits());
            else duration = (int) Math.ceil(course.getCredits());
            
            if (duration <= 0) duration = 3; // Fallback an toàn

            // C. Xác định Ca học (Start Periods)
            boolean isOnline = "ONLINE".equals(requiredRoomType);
            
            // Nếu Online -> Ưu tiên Ca tối (Tiết 13), sau đó mới đến các ca ngày
            // Nếu Offline -> Chỉ các kíp chuẩn ban ngày (1, 4, 7, 10)
            int[] startPeriods = isOnline ? new int[]{13, 1, 4, 7, 10} : new int[]{1, 4, 7, 10};

            boolean assigned = false;

            // Loop Ngày
            daysLoop:
            for (int day = 2; day <= 8; day++) { // Thứ 2 -> CN
                // Nếu không phải Online, hạn chế xếp CN (trừ khi không còn cách nào khác)
                if (!isOnline && day == 8) continue; 

                for (int start : startPeriods) {
                    // Loop Phòng
                    for (Room room : rooms) {
                        
                        // 1. Check Loại phòng (QUAN TRỌNG)
                        // Lớp TH phải vào Lab, Lớp LT phải vào Giảng đường
                        if (!room.getType().equalsIgnoreCase(requiredRoomType)) continue;

                        // 2. Check Sức chứa
                        if (room.getCapacity() < offering.getPlannedSize()) continue;

                        // 3. Check Ràng buộc cứng (Trùng lịch, Trùng GV, Trùng Cha-Con)
                        // Gọi Validator đã update
                        if (validator.checkHardConstraints(offering, day, start, room, scheduledList)) {
                            
                            // Gán lịch thành công
                            offering.setDayOfWeek(day);
                            offering.setStartPeriod(start);
                            offering.setEndPeriod(start + duration - 1);
                            offering.setRoom(room);
                            offering.setStatus("SCHEDULED");
                            
                            scheduledList.add(offering);
                            assigned = true;
                            successCount++;
                            break daysLoop;
                        }
                    }
                }
            }

            // D. Xử lý khi thất bại (Diagnostic)
            if (!assigned) {
                offering.setStatus("ERROR");
                failCount++;
                
                // Phân tích nguyên nhân chi tiết để báo lại cho Admin
                analyzeFailureReason(offering, rooms, requiredRoomType, scheduledList);
            } else {
                offering.setStatusMessage("OK");
            }
        }

        offeringRepo.saveAll(offerings);
        return String.format("Heuristic Scheduling for '%s' completed.\nSuccess: %d\nFailed: %d", currentSem.getName(), successCount, failCount);
    }

    // Hàm phụ trợ: Phân tích nguyên nhân lỗi
    private void analyzeFailureReason(CourseOffering offering, List<Room> rooms, String requiredType, List<CourseOffering> scheduledList) {
        // 1. Kiểm tra xem có phòng nào đủ sức chứa và đúng loại không (bỏ qua lịch trống)
        long validRoomsCount = rooms.stream()
                .filter(r -> r.getType().equalsIgnoreCase(requiredType) && r.getCapacity() >= offering.getPlannedSize())
                .count();

        if (validRoomsCount == 0) {
            offering.setStatusMessage("No " + requiredType + " room capacity >= " + offering.getPlannedSize());
            return;
        }

        // 2. Nếu có phòng, thử tìm lý do conflict cụ thể với phòng đầu tiên phù hợp
        Room candidate = rooms.stream()
                .filter(r -> r.getType().equalsIgnoreCase(requiredType) && r.getCapacity() >= offering.getPlannedSize())
                .findFirst().orElse(null);
        
        if (candidate != null) {
                // Giả lập thử xếp vào Thứ 2, Tiết 1 xem lỗi gì
                String reason = validator.getConflictDetail(offering, 2, 1, candidate, scheduledList);
                if (reason != null) {
                    offering.setStatusMessage("Conflict example: " + reason);
                } else {
                    offering.setStatusMessage("Scheduling Constraint Failed (Complex overlap)");
                }
        }
    }
}