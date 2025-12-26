package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.algorithm.TimeTableValidator;
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
    @Autowired private SemesterRepository semesterRepo; // Để lấy kỳ hiện tại
    @Autowired private TimeTableValidator validator;

    @Transactional
    public String generateSchedule() {
        // 0. Chỉ xếp lịch cho kỳ hiện tại (Active Semester)
        Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
        if (currentSem == null) return "No active semester found!";

        // Lấy offerings của kỳ này (Bạn cần update Repo để findBySemester)
        // Tạm thời lấy tất cả, nhưng logic chuẩn là phải filter theo kỳ
        List<CourseOffering> offerings = offeringRepo.findAll(); 
        List<Room> rooms = roomRepo.findAll();

        // Calculate max lab capacity
        int maxLabCapacity = rooms.stream()
            .filter(r -> r.getType().equalsIgnoreCase("LAB"))
            .mapToInt(Room::getCapacity)
            .max().orElse(40);
        
        // Reset trạng thái & Auto-fix Lab Size
        for (CourseOffering o : offerings) {
            boolean isLab = o.getCourse().getName().toLowerCase().contains("thực hành") 
                         || o.getCourse().getName().toLowerCase().contains("(th)")
                         || (o.getCourse().getPracticeCredits() != null && o.getCourse().getPracticeCredits() > 0);
            
            if (isLab && o.getPlannedSize() > maxLabCapacity) {
                o.setPlannedSize(maxLabCapacity);
            }

            o.setStatus("PLANNED");
            o.setStatusMessage(null);
            o.setDayOfWeek(null);
            o.setStartPeriod(null);
            o.setEndPeriod(null);
            o.setRoom(null);
        }

        // Sort phòng: Bé trước, Lớn sau để tiết kiệm phòng lớn
        rooms.sort(Comparator.comparingInt(Room::getCapacity));

        // Sort lớp học: Lớp khó xếp (đông, nhiều tín chỉ) xếp trước
        offerings.sort((o1, o2) -> o2.getPlannedSize() - o1.getPlannedSize());

        List<CourseOffering> scheduledList = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;

        for (CourseOffering offering : offerings) {
            // Xác định thời lượng
            int credits = offering.getCourse().getCredits();
            int duration = (credits > 0) ? credits : 3; // Mặc định 3 tiết nếu ko có data

            boolean assigned = false;
            
            // XÁC ĐỊNH CA HỌC DỰA TRÊN LOẠI PHÒNG (Online -> Tối)
            // Nếu muốn xếp Online vào tối, ta check loại phòng hoặc tên môn
            boolean isOnline = false;
            if (offering.getRoom() != null && offering.getRoom().getType().equals("ONLINE")) isOnline = true;
            // Hoặc check tên môn nếu chưa gán phòng
            if (offering.getCourse().getName().toUpperCase().contains("ONLINE")) isOnline = true;

            // Xác định các Slot bắt đầu hợp lệ (Standard Time Blocks)
            int[] startPeriods;
            
            if (isOnline) {
                // Ca tối: Bắt đầu tiết 13 (19h)
                startPeriods = new int[]{13}; 
            } else {
                // Ca ngày: 1, 4, 7, 10
                startPeriods = new int[]{1, 4, 7, 10};
            }

            // Loop Ngày
            daysLoop:
            for (int day = 2; day <= 8; day++) { // Từ Thứ 2 đến CN (CN thường cho Online)
                
                // Nếu Online ưu tiên xếp CN hoặc buổi tối các ngày
                if (isOnline && day < 8 && startPeriods[0] != 13) continue; 

                for (int start : startPeriods) {
                    
                    // Loop Phòng
                    for (Room room : rooms) {
                        
                        // Nếu là lớp Online, chỉ xếp vào phòng ảo
                        if (isOnline && !room.getType().equals("ONLINE")) continue;
                        // Nếu lớp Offline, không xếp vào phòng ảo
                        if (!isOnline && room.getType().equals("ONLINE")) continue;

                        // Check Ràng buộc
                        if (validator.checkHardConstraints(offering, day, start, room, scheduledList)) {
                            
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

            if (!assigned) {
                offering.setStatus("ERROR");
                
                // Diagnostic
                boolean hasCapacity = rooms.stream().anyMatch(r -> r.getCapacity() >= offering.getPlannedSize());
                
                boolean isLab = offering.getCourse().getName().toLowerCase().contains("thực hành") 
                             || offering.getCourse().getName().toLowerCase().contains("(th)")
                             || (offering.getCourse().getPracticeCredits() != null && offering.getCourse().getPracticeCredits() > 0);
                
                boolean finalIsOnline = isOnline; // effectively final for lambda
                boolean hasType = rooms.stream().anyMatch(r -> {
                    if (finalIsOnline) return r.getType().equals("ONLINE");
                    if (isLab) return r.getType().equalsIgnoreCase("LAB");
                    return !r.getType().equalsIgnoreCase("LAB") && !r.getType().equals("ONLINE");
                });

                if (!hasCapacity) {
                    offering.setStatusMessage("No room has enough capacity (" + offering.getPlannedSize() + ")");
                } else if (!hasType) {
                    offering.setStatusMessage("No room of required type found");
                } else {
                    // Try to find a specific conflict reason
                    String conflictReason = "Time/Room conflict (Unknown)";
                    
                    // Find a candidate room that satisfies hard constraints
                    Room candidateRoom = rooms.stream()
                        .filter(r -> r.getCapacity() >= offering.getPlannedSize())
                        .filter(r -> {
                             if (finalIsOnline) return r.getType().equals("ONLINE");
                             if (isLab) return r.getType().equalsIgnoreCase("LAB");
                             return !r.getType().equalsIgnoreCase("LAB") && !r.getType().equals("ONLINE");
                        })
                        .findFirst().orElse(null);

                    if (candidateRoom == null) {
                        offering.setStatusMessage("No room matches BOTH Capacity (" + offering.getPlannedSize() + ") and Type (" + (isLab ? "LAB" : "THEORY") + ")");
                    } else {
                        // Check why this room failed in standard slots
                        // Include 13 (Evening) for offline classes too, as user has defined it in time_slots
                        int[] checkStarts = finalIsOnline ? new int[]{13} : new int[]{1, 4, 7, 10, 13};
                        // Check first valid day/slot
                        for (int d = 2; d <= 8; d++) { // Check up to Sunday
                             for (int s : checkStarts) {
                                 String reason = validator.getConflictDetail(offering, d, s, candidateRoom, scheduledList);
                                 if (reason != null) {
                                     conflictReason = reason;
                                     break;
                                 }
                             }
                             if (!conflictReason.equals("Time/Room conflict (Unknown)")) break;
                        }
                        
                        if (conflictReason.equals("Time/Room conflict (Unknown)")) {
                             conflictReason = "Diagnostic Error: Room " + candidateRoom.getName() + " appears valid but was not selected.";
                        }
                        offering.setStatusMessage(conflictReason);
                    }
                }

                failCount++;
            } else {
                offering.setStatusMessage("OK");
            }
        }

        offeringRepo.saveAll(offerings);
        return String.format("Scheduling for %s completed. Success: %d. Failed: %d", currentSem.getName(), successCount, failCount);
    }
}