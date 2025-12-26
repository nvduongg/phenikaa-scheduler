package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.SemesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SemesterService {
    @Autowired private SemesterRepository semesterRepo;

    public List<Semester> getAll() {
        return semesterRepo.findAll();
    }

    public Semester createOrUpdate(Semester semester) {
        if (semester.getIsCurrent() != null && semester.getIsCurrent()) {
            semesterRepo.resetAllCurrent(); // Đảm bảo chỉ có 1 kỳ active
        }
        return semesterRepo.save(semester);
    }
    
    public void setAsCurrent(Long id) {
        semesterRepo.resetAllCurrent();
        Semester s = semesterRepo.findById(id).orElseThrow();
        s.setIsCurrent(true);
        semesterRepo.save(s);
    }
    
    public Semester getCurrentSemester() {
        return semesterRepo.findByIsCurrentTrue().orElse(null);
    }
}