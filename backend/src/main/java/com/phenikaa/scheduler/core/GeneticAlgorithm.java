package com.phenikaa.scheduler.core;

import com.phenikaa.scheduler.validator.TimeTableValidator;
import com.phenikaa.scheduler.model.CourseOffering;
import com.phenikaa.scheduler.model.Room;
import com.phenikaa.scheduler.repository.CourseOfferingRepository;
import com.phenikaa.scheduler.repository.RoomRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeneticAlgorithm {

    @Autowired private CourseOfferingRepository offeringRepo;
    @Autowired private RoomRepository roomRepo;
    @SuppressWarnings("unused")
    @Autowired private TimeTableValidator validator;

    private static final int POPULATION_SIZE = 100; // Tăng lên chút để đa dạng
    private static final int GENERATIONS = 200;     // Chạy lâu hơn để tìm phương án tốt
    private static final double MUTATION_RATE = 0.15;
    private static final int TOURNAMENT_SIZE = 5;

    @Data
    @AllArgsConstructor
    class Schedule {
        Map<Long, Gene> genes; 
        double fitness;
        boolean isFitnessChanged;

        public Schedule() {
            this.genes = new HashMap<>();
            this.isFitnessChanged = true;
        }
        
        public Schedule(Schedule other) {
            this.genes = new HashMap<>();
            for (Map.Entry<Long, Gene> entry : other.genes.entrySet()) {
                this.genes.put(entry.getKey(), new Gene(entry.getValue()));
            }
            this.fitness = other.fitness;
            this.isFitnessChanged = other.isFitnessChanged;
        }
    }

    @Data
    @AllArgsConstructor
    class Gene {
        int day;
        int startPeriod;
        Room room;
        
        public Gene(Gene other) {
            this.day = other.day;
            this.startPeriod = other.startPeriod;
            this.room = other.room; 
        }
    }

    public String runGeneticAlgorithm(Long semesterId) {
        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(semesterId);
        List<Room> rooms = roomRepo.findAll();

        if (offerings.isEmpty()) return "No offerings to schedule.";

        List<Schedule> population = initializePopulation(offerings, rooms);
        Schedule bestSchedule = population.get(0);

        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Parallel stream để tính điểm nhanh hơn
            population.parallelStream().forEach(sch -> {
                if (sch.isFitnessChanged) calculateFitness(sch, offerings);
            });

            population.sort((s1, s2) -> Double.compare(s2.fitness, s1.fitness));
            
            if (population.get(0).fitness > bestSchedule.fitness) {
                bestSchedule = new Schedule(population.get(0));
            }

            if (generation % 20 == 0) {
                System.out.println("Gen " + generation + " | Best Fitness: " + population.get(0).fitness);
            }
            
            if (population.get(0).fitness >= -10) break; 

            population = evolvePopulation(population, offerings, rooms);
        }

        saveSchedule(bestSchedule, offerings);
        return "GA Completed. Best Fitness: " + bestSchedule.fitness;
    }

    private List<Schedule> initializePopulation(List<CourseOffering> offerings, List<Room> rooms) {
        List<Schedule> pop = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Schedule sch = new Schedule();
            for (CourseOffering off : offerings) {
                sch.genes.put(off.getId(), randomGene(off, rooms));
            }
            pop.add(sch);
        }
        return pop;
    }

    // --- LOGIC SINH GEN MỚI (QUAN TRỌNG NHẤT) ---
    private Gene randomGene(CourseOffering off, List<Room> rooms) {
        // 1. Xác định loại môn để chọn Kíp học (Slot)
        boolean isOnlineType = isOnlineCourse(off);

        // Kíp chuẩn: 1, 4, 7, 10, 13
        int[] validSlots;
        if (isOnlineType) {
            // ELN/Coursera: CHỈ học tối (Tiết 13)
            validSlots = new int[]{13}; 
        } else {
            // Môn thường: Ưu tiên học ngày (1, 4, 7, 10). Hạn chế tối.
            // Có thể thêm 13 nếu muốn môn thường học tối, nhưng ở đây ta làm chặt theo quy chế
            validSlots = new int[]{1, 4, 7, 10}; 
        }
        
        int startPeriod = validSlots[(int)(Math.random() * validSlots.length)];
        
        // 2. Chọn ngày (Online học cả tuần kể cả CN, Offline hạn chế CN)
        int day;
        if (isOnlineType) {
            day = 2 + (int)(Math.random() * 7); // Thứ 2 -> CN (2-8)
        } else {
            day = 2 + (int)(Math.random() * 6); // Thứ 2 -> T7 (2-7)
        }

        // 3. Chọn phòng (Lọc phòng đúng loại)
        String reqType = getRequiredRoomType(off);
        List<Room> validRooms = rooms.stream()
                .filter(r -> r.getType().equalsIgnoreCase(reqType) && r.getCapacity() >= off.getPlannedSize())
                .collect(Collectors.toList());
        
        // Fallback: Nếu không có phòng thỏa mãn, lấy tạm phòng bất kỳ đúng loại (dù thiếu chỗ) để GA tự phạt sau
        if (validRooms.isEmpty()) {
            validRooms = rooms.stream().filter(r -> r.getType().equalsIgnoreCase(reqType)).collect(Collectors.toList());
        }
        if (validRooms.isEmpty()) validRooms = rooms; // Fallback cuối cùng

        Room room = validRooms.get((int)(Math.random() * validRooms.size()));

        return new Gene(day, startPeriod, room);
    }
    
    // Hàm helper kiểm tra môn Online/Coursera
    private boolean isOnlineCourse(CourseOffering off) {
        @SuppressWarnings("unused")
        String code = off.getCourse().getCourseCode().toUpperCase();
        String name = off.getCourse().getName().toUpperCase();
        String type = off.getClassType() != null ? off.getClassType().toUpperCase() : "";

        return type.equals("ELN") 
            || type.equals("COURSERA") // Check thêm Coursera
            || name.contains("COURSERA")
            || name.contains("TRỰC TUYẾN")
            || Boolean.TRUE.equals(off.getCourse().getIsOnline());
    }

    private String getRequiredRoomType(CourseOffering off) {
        if (isOnlineCourse(off)) return "ONLINE";
        if ("TH".equalsIgnoreCase(off.getClassType())) return "LAB";
        return "THEORY";
    }

    private void calculateFitness(Schedule schedule, List<CourseOffering> allOfferings) {
        double score = 0;
        Map<String, Long> roomOccupancy = new HashMap<>();
        Map<String, Long> lecturerOccupancy = new HashMap<>();
        Map<String, Long> classOccupancy = new HashMap<>(); // Check trùng lớp biên chế

        for (CourseOffering off : allOfferings) {
            Gene gene = schedule.genes.get(off.getId());
            
            // Tính duration (làm tròn lên)
            int duration = (int)Math.ceil(off.getCourse().getCredits());
            // Quy chế: Thường là 3 tiết/ca. Nếu tín chỉ ít hơn vẫn chiếm slot đó?
            // Ở đây ta dùng đúng tín chỉ, nhưng slot bắt đầu đã cố định 1,4,7,10
            if (duration <= 0) duration = 3; 

            // 1. Phạt nếu sai Slot quy định (Double check)
            boolean isOnline = isOnlineCourse(off);
            if (isOnline && gene.startPeriod != 13) score -= 1000; // Phạt nặng
            if (!isOnline && gene.startPeriod == 13) score -= 200; // Phạt nhẹ hơn (Môn thường học tối không khuyến khích)
            if (!Arrays.asList(1, 4, 7, 10, 13).contains(gene.startPeriod)) score -= 500; // Sai kíp

            // 2. Phạt Sức chứa & Loại phòng
            if (!gene.room.getType().equalsIgnoreCase(getRequiredRoomType(off))) score -= 1000;
            if (gene.room.getCapacity() < off.getPlannedSize()) score -= 500;

            // 3. Check Trùng lặp
            for (int t = 0; t < duration; t++) {
                int period = gene.startPeriod + t;
                
                // Trùng Phòng
                if (!gene.room.getType().equalsIgnoreCase("ONLINE")) {
                    String roomKey = gene.day + "-" + period + "-" + gene.room.getId();
                    if (roomOccupancy.containsKey(roomKey)) score -= 1000;
                    else roomOccupancy.put(roomKey, off.getId());
                }

                // Trùng GV
                if (off.getLecturer() != null) {
                    String lecKey = gene.day + "-" + period + "-" + off.getLecturer().getId();
                    if (lecturerOccupancy.containsKey(lecKey)) score -= 1000;
                    else lecturerOccupancy.put(lecKey, off.getId());
                }

                // Trùng Lớp biên chế (Sinh viên)
                if (off.getTargetClasses() != null && !off.getTargetClasses().isEmpty()) {
                     // Tách chuỗi lớp: "K16-CNTT1;K16-CNTT2"
                     String[] classes = off.getTargetClasses().split(";");
                     for (String cls : classes) {
                         String classKey = gene.day + "-" + period + "-" + cls.trim();
                         if (classOccupancy.containsKey(classKey)) score -= 500;
                         else classOccupancy.put(classKey, off.getId());
                     }
                }
            }
            
            // 4. Ràng buộc Cha - Con (Parent-Child)
            if (off.getParent() != null) {
                Gene parentGene = schedule.genes.get(off.getParent().getId());
                if (parentGene != null && parentGene.day == gene.day) {
                    // Check overlap
                    int parentDuration = (int)Math.ceil(off.getParent().getCourse().getCredits());
                    int parentEnd = parentGene.startPeriod + parentDuration - 1;
                    int childEnd = gene.startPeriod + duration - 1;
                    
                    if (gene.startPeriod <= parentEnd && childEnd >= parentGene.startPeriod) {
                        score -= 2000; // Con trùng giờ Cha -> Không thể chấp nhận
                    }
                }
            }
        }
        schedule.fitness = score;
        schedule.isFitnessChanged = false;
    }

    private List<Schedule> evolvePopulation(List<Schedule> pop, List<CourseOffering> offerings, List<Room> rooms) {
        List<Schedule> newPop = new ArrayList<>();
        // Giữ top 10%
        int eliteCount = (int)(POPULATION_SIZE * 0.1);
        for (int i = 0; i < eliteCount; i++) newPop.add(new Schedule(pop.get(i)));

        while (newPop.size() < POPULATION_SIZE) {
            Schedule p1 = tournamentSelection(pop);
            Schedule p2 = tournamentSelection(pop);
            Schedule child = crossover(p1, p2, offerings);
            mutate(child, offerings, rooms);
            newPop.add(child);
        }
        return newPop;
    }

    private Schedule tournamentSelection(List<Schedule> pop) {
        Schedule best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Schedule ind = pop.get((int)(Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private Schedule crossover(Schedule p1, Schedule p2, List<CourseOffering> offerings) {
        Schedule child = new Schedule();
        for (CourseOffering off : offerings) {
            // Uniform Crossover
            child.genes.put(off.getId(), Math.random() < 0.5 ? 
                new Gene(p1.genes.get(off.getId())) : new Gene(p2.genes.get(off.getId())));
        }
        return child;
    }

    private void mutate(Schedule child, List<CourseOffering> offerings, List<Room> rooms) {
        for (CourseOffering off : offerings) {
            if (Math.random() < MUTATION_RATE) {
                // Đột biến: Random lại theo đúng quy tắc Kíp học
                child.genes.put(off.getId(), randomGene(off, rooms));
            }
        }
        child.isFitnessChanged = true;
    }

    private void saveSchedule(Schedule best, List<CourseOffering> offerings) {
        List<CourseOffering> toSave = new ArrayList<>();
        for (CourseOffering off : offerings) {
            Gene gene = best.genes.get(off.getId());
            off.setDayOfWeek(gene.day);
            off.setStartPeriod(gene.startPeriod);
            
            int duration = (int)Math.ceil(off.getCourse().getCredits());
            if (duration <= 0) duration = 3;
            off.setEndPeriod(gene.startPeriod + duration - 1);
            
            off.setRoom(gene.room);
            off.setStatus(best.fitness >= -100 ? "SCHEDULED" : "ERROR");
            off.setStatusMessage("GA Fitness: " + best.fitness);
            toSave.add(off);
        }
        offeringRepo.saveAll(toSave);
    }
}