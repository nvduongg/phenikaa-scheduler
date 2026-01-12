package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.core.GeneticAlgorithm;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.SemesterRepository;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
public class SchedulerService {
    @Autowired private SemesterRepository semesterRepo;
    @Autowired private CourseOfferingRepository offeringRepo;
    
    @Autowired private GeneticAlgorithm geneticAlgorithm;

    @Transactional
    public String generateSchedule(String algorithm) {
        return generateSchedule((Long) null);
    }

    @Transactional
    public String generateSchedule(Long semesterId) {
        Semester sem;
        if (semesterId != null) {
            sem = semesterRepo.findById(semesterId).orElse(null);
            if (sem == null) return "Error: Semester not found (id=" + semesterId + ")";
        } else {
            sem = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (sem == null) return "Error: No active semester found! Please activate a semester first.";
        }

        // Migrate legacy data: nếu semester có offerings=0 nhưng DB đang có offerings chưa gán semester
        // thì gán tạm vào semester đang chạy để GA có dữ liệu.
        if (offeringRepo.findBySemester_Id(sem.getId()).isEmpty()) {
            List<CourseOffering> missing = offeringRepo.findBySemesterIsNull();
            if (missing != null && !missing.isEmpty()) {
                missing.forEach(o -> o.setSemester(sem));
                offeringRepo.saveAll(missing);
            }
        }

        return geneticAlgorithm.run(sem.getId());
    }

    public Workbook buildTimetableWorkbook(String sheetName, List<CourseOffering> offerings) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Timetable");

        String[] cols = {
                "Class Code",
                "Course Code",
                "Course Name",
                "Type",
                "Target Classes",
                "Planned Size",
                "Lecturer",
                "Room",
                "Room Type",
                "Day",
                "Start Period",
                "End Period",
                "Status"
        };

        CellStyle headerStyle = ExcelTemplateUtil.createBoldHeaderStyle(workbook);
        ExcelTemplateUtil.createHeaderRow(sheet, cols, headerStyle, 22);

        List<CourseOffering> sorted = offerings.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((CourseOffering o) -> nullSafeInt(o.getDayOfWeek()))
                        .thenComparing(o -> nullSafeInt(o.getStartPeriod()))
                        .thenComparing(o -> nullSafe(o.getRoom() != null ? o.getRoom().getName() : null))
                        .thenComparing(o -> nullSafe(o.getCode())))
                .toList();

        int rowIdx = 1;
        for (CourseOffering o : sorted) {
            Row row = sheet.createRow(rowIdx++);

            row.createCell(0).setCellValue(nullSafe(o.getCode()));
            row.createCell(1).setCellValue(nullSafe(o.getCourse() != null ? o.getCourse().getCourseCode() : null));
            row.createCell(2).setCellValue(nullSafe(o.getCourse() != null ? o.getCourse().getName() : null));
            row.createCell(3).setCellValue(nullSafe(o.getClassType()));
            row.createCell(4).setCellValue(nullSafe(o.getTargetClasses()));

            if (o.getPlannedSize() != null) row.createCell(5).setCellValue(o.getPlannedSize());
            else row.createCell(5).setCellValue("");

            row.createCell(6).setCellValue(nullSafe(o.getLecturer() != null ? o.getLecturer().getFullName() : null));
            row.createCell(7).setCellValue(nullSafe(o.getRoom() != null ? o.getRoom().getName() : null));
            row.createCell(8).setCellValue(nullSafe(o.getRoom() != null ? o.getRoom().getType() : null));

            row.createCell(9).setCellValue(toDayLabel(o.getDayOfWeek()));

            if (o.getStartPeriod() != null) row.createCell(10).setCellValue(o.getStartPeriod());
            else row.createCell(10).setCellValue("");

            if (o.getEndPeriod() != null) row.createCell(11).setCellValue(o.getEndPeriod());
            else row.createCell(11).setCellValue("");

            row.createCell(12).setCellValue(nullSafe(o.getStatus()));
        }

        return workbook;
    }

    private static String toDayLabel(Integer dayOfWeek) {
        if (dayOfWeek == null) return "";
        // GeneticAlgorithm uses 2-8 mapping.
        return switch (dayOfWeek) {
            case 2 -> "Mon";
            case 3 -> "Tue";
            case 4 -> "Wed";
            case 5 -> "Thu";
            case 6 -> "Fri";
            case 7 -> "Sat";
            case 8 -> "Sun";
            default -> String.valueOf(dayOfWeek);
        };
    }

    private static String nullSafe(String s) {
        if (s == null) return "";
        return s.trim();
    }

    private static int nullSafeInt(Integer i) {
        return i != null ? i : Integer.MAX_VALUE;
    }
}