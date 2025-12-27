package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.core.GeneticAlgorithm;
import com.phenikaa.scheduler.service.SchedulerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduler")
@CrossOrigin(origins = "http://localhost:5173") // Cho phép Frontend truy cập
public class SchedulerController {

    @Autowired private SchedulerService heuristicService; // Thuật toán Tham lam (Cũ)
    @Autowired private GeneticAlgorithm geneticService;  // Thuật toán Di truyền (Mới)
    @Autowired private SemesterRepository semesterRepo;

    /**
     * API 1: Chạy thuật toán Heuristic (Nhanh, dựa trên luật)
     * Thường dùng để xếp lịch sơ bộ hoặc test nhanh.
     */
    @PostMapping("/run-heuristic")
    // @PreAuthorize("hasAuthority('ADMIN')") // Chỉ Admin trường mới được xếp
    public ResponseEntity<String> runHeuristicScheduler() {
        try {
            long startTime = System.currentTimeMillis();
            
            String result = heuristicService.generateSchedule();
            
            long duration = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(result + " (Execution Time: " + duration + "ms)");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Heuristic Scheduling Failed: " + e.getMessage());
        }
    }

    /**
     * API 2: Chạy thuật toán Genetic Algorithm (Chậm hơn, tối ưu hóa điểm phạt)
     * Dùng để tìm phương án tối ưu khi thuật toán thường bị kẹt.
     */
    @PostMapping("/run-genetic")
    // @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> runGeneticScheduler() {
        try {
            // Lấy kỳ học hiện tại
            Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (currentSem == null) {
                return ResponseEntity.badRequest().body("No active semester found! Please activate a semester.");
            }

            long startTime = System.currentTimeMillis();
            
            // Chạy GA
            String result = geneticService.runGeneticAlgorithm(currentSem.getId());
            
            long duration = System.currentTimeMillis() - startTime;
            return ResponseEntity.ok(result + " (Execution Time: " + duration + "ms)");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Genetic Algorithm Failed: " + e.getMessage());
        }
    }
}