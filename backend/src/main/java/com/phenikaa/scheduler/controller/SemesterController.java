package com.phenikaa.scheduler.controller;

import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.service.SemesterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/semesters")
@CrossOrigin(origins = "http://localhost:5173")
public class SemesterController {
    @Autowired private SemesterService semesterService;

    @GetMapping
    public ResponseEntity<List<Semester>> getAll() {
        return ResponseEntity.ok(semesterService.getAll());
    }

    @PostMapping
    public ResponseEntity<Semester> create(@RequestBody Semester semester) {
        return ResponseEntity.ok(semesterService.createOrUpdate(semester));
    }
    
    @PostMapping("/{id}/set-current")
    public ResponseEntity<Void> setAsCurrent(@PathVariable Long id) {
        semesterService.setAsCurrent(id);
        return ResponseEntity.ok().build();
    }
}