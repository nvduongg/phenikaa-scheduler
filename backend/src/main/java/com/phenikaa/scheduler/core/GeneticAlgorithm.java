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

    // CẤU HÌNH GA
    private static final int POPULATION_SIZE = 50;   // Số lượng cá thể trong quần thể
    private static final int GENERATIONS = 100;      // Số thế hệ chạy
    private static final double MUTATION_RATE = 0.1; // Tỷ lệ đột biến (10%)
    private static final int TOURNAMENT_SIZE = 5;

    // Class đại diện cho 1 Nhiễm sắc thể (Một phương án Thời khóa biểu trọn vẹn)
    @Data
    @AllArgsConstructor
    class Schedule {
        // Map: OfferingID -> [Day, StartPeriod, RoomID]
        Map<Long, Gene> genes; 
        double fitness;
        boolean isFitnessChanged;

        public Schedule() {
            this.genes = new HashMap<>();
            this.isFitnessChanged = true;
        }
        
        // Copy constructor
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
            this.room = other.room; // Room là object reference, không cần deep copy vì Room tĩnh
        }
    }

    public String runGeneticAlgorithm(Long semesterId) {
        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(semesterId);
        List<Room> rooms = roomRepo.findAll();

        if (offerings.isEmpty()) return "No offerings to schedule.";

        // 1. Khởi tạo quần thể ban đầu (Random)
        List<Schedule> population = initializePopulation(offerings, rooms);

        // 2. Vòng lặp tiến hóa
        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Tính điểm cho toàn bộ quần thể
            population.forEach(sch -> {
                if (sch.isFitnessChanged) calculateFitness(sch, offerings, rooms);
            });

            // Sắp xếp giảm dần theo Fitness (Điểm cao nhất ở đầu)
            population.sort((s1, s2) -> Double.compare(s2.fitness, s1.fitness));

            // Log tiến độ
            if (generation % 10 == 0) {
                System.out.println("Gen " + generation + " | Best Fitness: " + population.get(0).fitness);
            }
            
            // Nếu tìm thấy phương án hoàn hảo (Điểm = 0 hoặc chấp nhận được)
            if (population.get(0).fitness >= -10) break; // -10 là ngưỡng chấp nhận lỗi nhỏ

            // Lai ghép & Đột biến để tạo thế hệ mới
            population = evolvePopulation(population, offerings, rooms);
        }

        // 3. Lưu kết quả tốt nhất
        Schedule bestSchedule = population.get(0);
        saveSchedule(bestSchedule, offerings);

        return "GA Completed. Best Fitness: " + bestSchedule.fitness;
    }

    // --- CÁC HÀM HỖ TRỢ GA ---

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

    private Gene randomGene(CourseOffering off, List<Room> rooms) {
        // Random Day (2-7, CN hạn chế)
        int day = 2 + (int)(Math.random() * 6); 
        // Random Slot (1, 4, 7, 10, 13)
        int[] slots = {1, 4, 7, 10, 13};
        int start = slots[(int)(Math.random() * slots.length)];
        
        // Random Room (Thông minh hơn: Chỉ random trong list phòng đúng loại)
        String reqType = getRequiredRoomType(off);
        List<Room> validRooms = rooms.stream()
                .filter(r -> r.getType().equalsIgnoreCase(reqType) && r.getCapacity() >= off.getPlannedSize())
                .collect(Collectors.toList());
        
        if (validRooms.isEmpty()) validRooms = rooms; // Fallback
        Room room = validRooms.get((int)(Math.random() * validRooms.size()));

        return new Gene(day, start, room);
    }
    
    private String getRequiredRoomType(CourseOffering off) {
        String type = off.getClassType();
        if ("TH".equals(type)) return "LAB";
        if ("ELN".equals(type) || Boolean.TRUE.equals(off.getCourse().getIsOnline())) return "ONLINE";
        return "THEORY";
    }

    // --- HÀM TÍNH ĐIỂM (FITNESS FUNCTION) - QUAN TRỌNG NHẤT ---
    private void calculateFitness(Schedule schedule, List<CourseOffering> allOfferings, List<Room> rooms) {
        double score = 0;
        
        // Map tạm để check trùng
        // Key: "Day-Period-RoomID" -> Value: OfferingID
        Map<String, Long> roomOccupancy = new HashMap<>();
        // Key: "Day-Period-LecturerID" -> Value: OfferingID
        Map<String, Long> lecturerOccupancy = new HashMap<>();
        
        // Duyệt qua từng gen để tính điểm
        for (CourseOffering off : allOfferings) {
            Gene gene = schedule.genes.get(off.getId());
            int duration = (int)Math.ceil(off.getCourse().getCredits());
            if (duration == 0) duration = 3;

            // 1. Ràng buộc Cứng: Loại phòng & Sức chứa
            if (!gene.room.getType().equalsIgnoreCase(getRequiredRoomType(off))) score -= 500;
            if (gene.room.getCapacity() < off.getPlannedSize()) score -= 500;

            // 2. Ràng buộc Cứng: Trùng lặp
            for (int t = 0; t < duration; t++) {
                int period = gene.startPeriod + t;
                
                // Check trùng Phòng
                String roomKey = gene.day + "-" + period + "-" + gene.room.getId();
                if (roomOccupancy.containsKey(roomKey)) score -= 1000; // Phạt cực nặng
                else roomOccupancy.put(roomKey, off.getId());

                // Check trùng Giảng viên
                if (off.getLecturer() != null) {
                    String lecKey = gene.day + "-" + period + "-" + off.getLecturer().getId();
                    if (lecturerOccupancy.containsKey(lecKey)) score -= 1000;
                    else lecturerOccupancy.put(lecKey, off.getId());
                }
            }
            
            // 3. Ràng buộc Cứng: Cha - Con (Parent - Child)
            if (off.getParent() != null) {
                Gene parentGene = schedule.genes.get(off.getParent().getId());
                if (parentGene != null && parentGene.day == gene.day) {
                    // Check overlap time
                    int parentEnd = parentGene.startPeriod + (int)Math.ceil(off.getParent().getCourse().getCredits()) - 1;
                    int childEnd = gene.startPeriod + duration - 1;
                    
                    if (gene.startPeriod <= parentEnd && childEnd >= parentGene.startPeriod) {
                        score -= 1000; // Con trùng giờ Cha
                    }
                }
            }
        }
        
        // Update Fitness
        schedule.fitness = score;
        schedule.isFitnessChanged = false;
    }

    private List<Schedule> evolvePopulation(List<Schedule> pop, List<CourseOffering> offerings, List<Room> rooms) {
        List<Schedule> newPop = new ArrayList<>();

        // Giữ lại top 10% ưu tú nhất (Elitism)
        int eliteCount = (int)(POPULATION_SIZE * 0.1);
        for (int i = 0; i < eliteCount; i++) newPop.add(new Schedule(pop.get(i)));

        // Lai ghép để tạo phần còn lại
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
            Schedule randomInd = pop.get((int)(Math.random() * pop.size()));
            if (best == null || randomInd.fitness > best.fitness) best = randomInd;
        }
        return best;
    }

    private Schedule crossover(Schedule p1, Schedule p2, List<CourseOffering> offerings) {
        Schedule child = new Schedule();
        // Uniform Crossover: Với mỗi môn, tung đồng xu chọn gen từ Cha hoặc Mẹ
        for (CourseOffering off : offerings) {
            if (Math.random() < 0.5) {
                child.genes.put(off.getId(), new Gene(p1.genes.get(off.getId())));
            } else {
                child.genes.put(off.getId(), new Gene(p2.genes.get(off.getId())));
            }
        }
        return child;
    }

    private void mutate(Schedule child, List<CourseOffering> offerings, List<Room> rooms) {
        for (CourseOffering off : offerings) {
            if (Math.random() < MUTATION_RATE) {
                // Đột biến: Random lại thời gian/phòng cho môn này
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
            if (duration == 0) duration = 3;
            off.setEndPeriod(gene.startPeriod + duration - 1);
            
            off.setRoom(gene.room);
            off.setStatus("SCHEDULED");
            if (best.fitness < -500) off.setStatusMessage("GA Result with Penalty: " + best.fitness);
            else off.setStatusMessage("GA Optimized");
            
            toSave.add(off);
        }
        offeringRepo.saveAll(toSave);
    }
}