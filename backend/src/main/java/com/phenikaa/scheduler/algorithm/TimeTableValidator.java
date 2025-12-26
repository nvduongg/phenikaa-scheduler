package com.phenikaa.scheduler.algorithm;

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
        int duration = offering.getCourse().getCredits(); 
        if (duration == 0) duration = 3; // Fallback nếu quên nhập tín chỉ
        int endPeriod = startPeriod + duration - 1;

        // 1. Kiểm tra sức chứa
        if (offering.getPlannedSize() > room.getCapacity()) {
            // Uncomment dòng dưới nếu muốn soi kỹ lỗi này
            // System.out.println("Fail Capacity: Class " + offering.getCode() + " (" + offering.getPlannedSize() + ") > Room " + room.getName() + " (" + room.getCapacity() + ")");
            return false;
        }

        // 2. Kiểm tra loại phòng (Logic quan trọng)
        boolean isLabCourse = offering.getCourse().getName().toLowerCase().contains("thực hành") 
                           || offering.getCourse().getName().toLowerCase().contains("(th)")
                           || offering.getCourse().getPracticeCredits() > 0;
        
        // Debug loại phòng
        // System.out.println("Checking " + offering.getCode() + " (IsLab: " + isLabCourse + ") vs Room " + room.getName() + " (Type: " + room.getType() + ")");

        if (isLabCourse && !room.getType().equalsIgnoreCase("LAB")) return false;
        if (!isLabCourse && room.getType().equalsIgnoreCase("LAB")) return false; 

        // 3. Kiểm tra trùng lịch
        for (CourseOffering existing : scheduledList) {
            if (existing.getDayOfWeek() == null || existing.getDayOfWeek() != day) continue;

            boolean timeOverlap = (startPeriod <= existing.getEndPeriod()) && (endPeriod >= existing.getStartPeriod());
            
            if (timeOverlap) {
                // Trùng phòng
                if (existing.getRoom().getId().equals(room.getId())) {
                    return false;
                }

                // Trùng giảng viên
                if (offering.getLecturer() != null && existing.getLecturer() != null) {
                    if (offering.getLecturer().getId().equals(existing.getLecturer().getId())) {
                        System.out.println("Fail Lecturer Conflict: " + offering.getLecturer().getFullName());
                        return false;
                    }
                }

                // Trùng sinh viên (Lớp biên chế)
                if (hasCommonClass(offering.getTargetClasses(), existing.getTargetClasses())) {
                    System.out.println("Fail Student Conflict: " + offering.getTargetClasses() + " vs " + existing.getTargetClasses());
                    return false;
                }
            }
        }

        return true; 
    }

    private boolean hasCommonClass(String target1, String target2) {
        if (target1 == null || target2 == null) return false;
        List<String> list1 = Arrays.asList(target1.split(";"));
        List<String> list2 = Arrays.asList(target2.split(";"));
        for (String c1 : list1) {
            for (String c2 : list2) {
                if (c1.trim().equalsIgnoreCase(c2.trim())) return true;
            }
        }
        return false;
    }

    public String getConflictDetail(
            CourseOffering offering, 
            int day, 
            int startPeriod, 
            Room room, 
            List<CourseOffering> scheduledList
    ) {
        int duration = offering.getCourse().getCredits(); 
        if (duration == 0) duration = 3;
        int endPeriod = startPeriod + duration - 1;

        for (CourseOffering existing : scheduledList) {
            if (existing.getDayOfWeek() == null || existing.getDayOfWeek() != day) continue;

            boolean timeOverlap = (startPeriod <= existing.getEndPeriod()) && (endPeriod >= existing.getStartPeriod());
            
            if (timeOverlap) {
                if (existing.getRoom().getId().equals(room.getId())) {
                    return "Room " + room.getName() + " occupied by " + existing.getCode();
                }

                if (offering.getLecturer() != null && existing.getLecturer() != null) {
                    if (offering.getLecturer().getId().equals(existing.getLecturer().getId())) {
                        return "Lecturer " + offering.getLecturer().getFullName() + " busy with " + existing.getCode();
                    }
                }

                if (hasCommonClass(offering.getTargetClasses(), existing.getTargetClasses())) {
                    return "Class " + existing.getTargetClasses() + " conflict with " + existing.getCode();
                }
            }
        }
        return null;
    }
}