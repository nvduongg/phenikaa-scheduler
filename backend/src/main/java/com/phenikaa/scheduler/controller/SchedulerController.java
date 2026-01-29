package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.service.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduler")
@CrossOrigin(origins = "http://localhost:5173")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    private final SemesterRepository semesterRepo;
    private final SchedulerService schedulerService;

    public SchedulerController(SemesterRepository semesterRepo, SchedulerService schedulerService) {
        this.semesterRepo = semesterRepo;
        this.schedulerService = schedulerService;
    }

    /**
     * API duy nhất để xếp lịch: Sử dụng Genetic Algorithm
     */
    @PostMapping("/generate")
    public ResponseEntity<String> generateSchedule(@RequestParam(required = false) Long semesterId) {
        try {
            long startTime = System.currentTimeMillis();

            // Chạy GA (theo semesterId nếu được truyền vào)
            String result = schedulerService.generateSchedule(semesterId);

            long duration = System.currentTimeMillis() - startTime;

            // Format kết quả trả về cho đẹp
            Semester semForMessage;
            if (semesterId != null) semForMessage = semesterRepo.findById(semesterId).orElse(null);
            else semForMessage = semesterRepo.findByIsCurrentTrue().orElse(null);

            String semLabel = semForMessage != null ? semForMessage.getName() : (semesterId != null ? String.valueOf(semesterId) : "CURRENT");
            return ResponseEntity.ok(String.format(
                    "Xếp lịch hoàn tất cho kỳ: %s\nKết quả: %s\nThời gian chạy: %d ms",
                    semLabel, result, duration
            ));

        } catch (Exception e) {
            log.error("Failed to generate schedule", e);
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}