package com.phenikaa.scheduler.service;

import com.phenikaa.scheduler.core.GeneticAlgorithm;
import com.phenikaa.scheduler.validator.*;
import com.phenikaa.scheduler.model.Course;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Room;
import com.phenikaa.scheduler.model.Semester;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.RoomRepository;
import com.phenikaa.scheduler.repository.SemesterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SchedulerService {

    @Autowired private CourseOfferingRepository offeringRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private SemesterRepository semesterRepo;
    @Autowired private TimeTableValidator validator;
    @Autowired private GeneticAlgorithm geneticAlgorithm;

    @Transactional
    public String generateSchedule(String algorithm) {
        // Luôn dùng GA, bỏ Heuristic cũ
        Semester currentSem = semesterRepo.findByIsCurrentTrue().orElse(null);
        if (currentSem == null) return "Error: No active semester found! Please activate a semester first.";
        return geneticAlgorithm.runGeneticAlgorithm(currentSem.getId());
    }
}