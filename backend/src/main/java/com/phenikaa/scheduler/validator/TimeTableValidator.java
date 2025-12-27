package com.phenikaa.scheduler.validator;

import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Room;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TimeTableValidator {

    public boolean checkHardConstraints(
            CourseOffering offering, 
            int day, 
            int startPeriod, 
            Room room, 
            List<CourseOffering> scheduledList
    ) {
        // 1. TÍNH THỜI LƯỢNG CHÍNH XÁC THEO LOẠI LỚP
        int duration = calculateDuration(offering);
        int endPeriod = startPeriod + duration - 1;

        // 2. KIỂM TRA SỨC CHỨA
        if (offering.getPlannedSize() > room.getCapacity()) {
            return false;
        }

        // 3. KIỂM TRA LOẠI PHÒNG (Strict Check)
        String type = offering.getClassType(); // LT, TH, ELN, ALL
        if (type == null) type = "ALL";

        // Logic check loại phòng
        if ("TH".equals(type) && !room.getType().equalsIgnoreCase("LAB")) return false; // TH bắt buộc vào Lab
        if ("LT".equals(type) && room.getType().equalsIgnoreCase("LAB")) return false;  // LT không được vào Lab (để dành chỗ)
        if ("ELN".equals(type) && !room.getType().equalsIgnoreCase("ONLINE")) return false; // Online bắt buộc vào phòng ảo
        
        // Nếu phòng là ONLINE nhưng lớp không phải ELN -> Sai
        if (room.getType().equalsIgnoreCase("ONLINE") && !"ELN".equals(type) && !Boolean.TRUE.equals(offering.getCourse().getIsOnline())) return false;


        // 4. KIỂM TRA TRÙNG LỊCH (Time Overlap)
        for (CourseOffering existing : scheduledList) {
            // Khác ngày thì không sao
            if (existing.getDayOfWeek() == null || existing.getDayOfWeek() != day) continue;

            // Kiểm tra giao nhau về thời gian
            boolean timeOverlap = (startPeriod <= existing.getEndPeriod()) && (endPeriod >= existing.getStartPeriod());
            
            if (timeOverlap) {
                // A. Trùng Phòng (Trừ khi là phòng Online ảo chứa vô tận)
                if (existing.getRoom().getId().equals(room.getId()) && !room.getType().equalsIgnoreCase("ONLINE")) {
                    return false;
                }

                // B. Trùng Giảng viên
                if (offering.getLecturer() != null && existing.getLecturer() != null) {
                    if (offering.getLecturer().getId().equals(existing.getLecturer().getId())) {
                        return false;
                    }
                }

                // C. Trùng Sinh viên (Lớp biên chế)
                if (hasCommonClass(offering.getTargetClasses(), existing.getTargetClasses())) {
                    return false;
                }

                // D. RÀNG BUỘC CHA - CON (QUAN TRỌNG NHẤT)
                // Trường hợp 1: Lớp hiện tại là CON, trùng giờ với lớp CHA của nó
                if (offering.getParent() != null && offering.getParent().getId().equals(existing.getId())) {
                    return false; 
                }
                // Trường hợp 2: Lớp hiện tại là CHA, trùng giờ với lớp CON của nó
                if (existing.getParent() != null && existing.getParent().getId().equals(offering.getId())) {
                    return false;
                }
            }
        }

        return true; 
    }

    // Helper tính thời lượng
    private int calculateDuration(CourseOffering offering) {
        String type = offering.getClassType();
        Double credits = 0.0;

        if ("LT".equals(type)) credits = offering.getCourse().getTheoryCredits();
        else if ("TH".equals(type)) credits = offering.getCourse().getPracticeCredits();
        else credits = offering.getCourse().getCredits(); // ALL hoặc ELN

        if (credits == null || credits == 0) return 3; // Fallback
        return (int) Math.ceil(credits);
    }

    private boolean hasCommonClass(String target1, String target2) {
        if (target1 == null || target2 == null) return false;
        // Tối ưu: Dùng contains nhanh trước khi split
        // Tuy nhiên split an toàn hơn để tránh K17-CNTT trùng K17-CNTT1
        List<String> list1 = Arrays.asList(target1.split(";"));
        List<String> list2 = Arrays.asList(target2.split(";"));
        for (String c1 : list1) {
            for (String c2 : list2) {
                if (c1.trim().equalsIgnoreCase(c2.trim())) return true;
            }
        }
        return false;
    }

    // Hàm trả về lý do lỗi (Dùng để hiển thị lên giao diện khi xếp lịch thất bại)
    public String getConflictDetail(
            CourseOffering offering, 
            int day, 
            int startPeriod, 
            Room room, 
            List<CourseOffering> scheduledList
    ) {
        int duration = calculateDuration(offering);
        int endPeriod = startPeriod + duration - 1;

        for (CourseOffering existing : scheduledList) {
            if (existing.getDayOfWeek() == null || existing.getDayOfWeek() != day) continue;

            boolean timeOverlap = (startPeriod <= existing.getEndPeriod()) && (endPeriod >= existing.getStartPeriod());
            
            if (timeOverlap) {
                if (existing.getRoom().getId().equals(room.getId()) && !room.getType().equalsIgnoreCase("ONLINE")) {
                    return "Room conflict with " + existing.getCode();
                }

                if (offering.getLecturer() != null && existing.getLecturer() != null) {
                    if (offering.getLecturer().getId().equals(existing.getLecturer().getId())) {
                        return "Lecturer busy with " + existing.getCode();
                    }
                }

                if (hasCommonClass(offering.getTargetClasses(), existing.getTargetClasses())) {
                    return "Class conflict with " + existing.getCode();
                }

                // Báo lỗi Cha-Con
                if (offering.getParent() != null && offering.getParent().getId().equals(existing.getId())) {
                    return "Conflict with Theory Class (Parent): " + existing.getCode();
                }
                if (existing.getParent() != null && existing.getParent().getId().equals(offering.getId())) {
                    return "Conflict with Practice Class (Child): " + existing.getCode();
                }
            }
        }
        return null; // Không conflict
    }
}