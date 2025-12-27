package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.core.GeneticAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduler")
@CrossOrigin(origins = "http://localhost:5173") // Cho phép Frontend truy cập
public class SchedulerController {
    @Autowired private GeneticAlgorithm geneticService;  // Thuật toán Di truyền (Mới)
    @Autowired private SemesterRepository semesterRepo;

    /**
     * API: Chạy thuật toán Genetic Algorithm (tối ưu hóa điểm phạt)
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