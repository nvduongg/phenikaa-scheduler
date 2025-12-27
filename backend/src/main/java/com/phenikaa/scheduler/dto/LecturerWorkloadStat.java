package com.phenikaa.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LecturerWorkloadStat {
    private Long lecturerId;
    private String lecturerName;
    private String email;
    private int totalClasses;       // Tổng số lớp phụ trách
    private long totalTheoryPeriods; // Tổng tiết lý thuyết
    private long totalPracticePeriods; // Tổng tiết thực hành
    private long totalPeriod;       // Tổng cộng (để vẽ biểu đồ)
}