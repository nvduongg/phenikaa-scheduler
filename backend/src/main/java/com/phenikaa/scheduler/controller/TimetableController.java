package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.controller.util.ExcelTemplateUtil;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.service.SchedulerService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/timetable")
@CrossOrigin(origins = "http://localhost:5173", exposedHeaders = {"Content-Disposition"})
public class TimetableController {

    private final SchedulerService schedulerService;
    private final CourseOfferingRepository offeringRepo;
    private final SemesterRepository semesterRepo;

    public TimetableController(
            SchedulerService schedulerService,
            CourseOfferingRepository offeringRepo,
            SemesterRepository semesterRepo
    ) {
        this.schedulerService = schedulerService;
        this.offeringRepo = offeringRepo;
        this.semesterRepo = semesterRepo;
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTimetable(@RequestParam(required = false) Long semesterId) throws IOException {
        Semester semester;
        if (semesterId == null) {
            semester = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (semester == null) {
                return ResponseEntity.badRequest().build();
            }
            semesterId = semester.getId();
        } else {
            semester = semesterRepo.findById(semesterId).orElse(null);
        }

        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(semesterId).stream()
            .filter(o -> o != null
                && "SCHEDULED".equals(o.getStatus())
                && o.getDayOfWeek() != null
                && o.getStartPeriod() != null
                && o.getEndPeriod() != null)
                .toList();

        String sheetName = "Timetable";
        String fileName = (semester != null && semester.getName() != null && !semester.getName().isBlank())
                ? ("Timetable_" + semester.getName().replaceAll("[^a-zA-Z0-9._-]+", "_") + ".xlsx")
                : ("Timetable_Semester_" + semesterId + ".xlsx");

        try (Workbook workbook = schedulerService.buildTimetableWorkbook(sheetName, offerings)) {
            return ExcelTemplateUtil.toXlsxResponse(workbook, fileName);
        }
    }
}
