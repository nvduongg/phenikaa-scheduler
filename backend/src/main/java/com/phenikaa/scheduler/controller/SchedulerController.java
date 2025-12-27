package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.core.GeneticAlgorithm;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduler")
@CrossOrigin(origins = "http://localhost:5173")
public class SchedulerController {

    private static final Logger log = LoggerFactory.getLogger(SchedulerController.class);

    // Chỉ giữ lại Core GA, bỏ heuristicService
    private final GeneticAlgorithm geneticAlgorithm;
    private final SemesterRepository semesterRepo;

    public SchedulerController(GeneticAlgorithm geneticAlgorithm, SemesterRepository semesterRepo) {
        this.geneticAlgorithm = geneticAlgorithm;
        this.semesterRepo = semesterRepo;
    }

    /**
     * API duy nhất để xếp lịch: Sử dụng Genetic Algorithm
     */
    @PostMapping("/generate")
    public ResponseEntity<String> generateSchedule() {
        try {
            // Lấy kỳ học hiện tại (Active Semester)
            Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (currentSem == null) {
                return ResponseEntity.badRequest().body("Lỗi: Không tìm thấy học kỳ đang kích hoạt (Active Semester)!");
            }

            long startTime = System.currentTimeMillis();
            
            // Chạy GA
            String result = geneticAlgorithm.run(currentSem.getId());
            
            long duration = System.currentTimeMillis() - startTime;
            
            // Format kết quả trả về cho đẹp
            return ResponseEntity.ok(String.format(
                "Xếp lịch hoàn tất cho kỳ: %s\nKết quả: %s\nThời gian chạy: %d ms", 
                currentSem.getName(), result, duration
            ));

        } catch (Exception e) {
            log.error("Failed to generate schedule", e);
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }
}