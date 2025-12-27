package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.dto.LecturerWorkloadStat;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import com.phenikaa.scheduler.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/statistics")
@CrossOrigin(origins = "http://localhost:5173")
public class StatisticsController {

    @Autowired private StatisticsService statsService;
    @Autowired private SemesterRepository semesterRepo;

    @GetMapping("/lecturer-workload")
    public ResponseEntity<?> getLecturerWorkload(@RequestParam(required = false) Long semesterId) {
        if (semesterId == null) {
            // Mặc định lấy kỳ hiện tại
            Semester current = semesterRepo.findByIsCurrentTrue().orElse(null);
            if (current == null) return ResponseEntity.badRequest().body("No active semester found");
            semesterId = current.getId();
        }
        
        List<LecturerWorkloadStat> data = statsService.getLecturerWorkload(semesterId);
        return ResponseEntity.ok(data);
    }
}