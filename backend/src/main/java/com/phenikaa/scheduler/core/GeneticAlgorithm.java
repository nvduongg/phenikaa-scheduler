package com.phenikaa.scheduler.core;

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
    
    // Không cần autowired Validator ở đây nếu ta nhúng logic check vào hàm tính điểm
    // Nhưng để clean, ta vẫn có thể dùng các hàm static helper hoặc giữ Validator

    // --- CẤU HÌNH GA ---
    private static final int POPULATION_SIZE = 150;   // Tăng lên để đa dạng hóa
    private static final int GENERATIONS = 300;       // Chạy sâu hơn
    private static final double MUTATION_RATE = 0.05; // Giảm nhẹ vì khởi tạo đã khá tốt
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
        int day;        // 2-8
        int startPeriod;// 1, 4, 7, 10, 13
        Room room;
        
        public Gene(Gene other) {
            this.day = other.day;
            this.startPeriod = other.startPeriod;
            this.room = other.room;
        }
    }

    // --- HÀM CHÍNH ---
    public String run(Long semesterId) {
        List<CourseOffering> offerings = offeringRepo.findBySemester_Id(semesterId);
        List<Room> rooms = roomRepo.findAll();

        if (offerings.isEmpty()) return "Không có lớp học phần nào để xếp.";

        // 1. Khởi tạo quần thể
        List<Schedule> population = initializePopulation(offerings, rooms);
        Schedule bestSchedule = population.get(0);

        // 2. Vòng lặp tiến hóa
        for (int generation = 0; generation < GENERATIONS; generation++) {
            // Parallel Stream để tăng tốc tính toán trên CPU đa nhân
            population.parallelStream().forEach(sch -> {
                if (sch.isFitnessChanged) calculateFitness(sch, offerings);
            });

            // Sắp xếp: Fitness cao nhất (gần 0 nhất) lên đầu
            population.sort((s1, s2) -> Double.compare(s2.fitness, s1.fitness));
            
            // Cập nhật Best Schedule
            if (population.get(0).fitness > bestSchedule.fitness) {
                bestSchedule = new Schedule(population.get(0));
            }

            // Log tiến độ (mỗi 50 thế hệ)
            if (generation % 50 == 0) {
                System.out.println("GA Gen " + generation + " | Best Fitness: " + bestSchedule.fitness);
            }
            
            // Điều kiện dừng sớm: Nếu fitness >= -10 (Gần như hoàn hảo)
            if (bestSchedule.fitness >= -10) break; 

            // Tạo thế hệ mới
            population = evolvePopulation(population, offerings, rooms);
        }

        // 3. Lưu kết quả
        saveSchedule(bestSchedule, offerings);
        return "Best Fitness Score: " + bestSchedule.fitness;
    }

    // --- LOGIC KHỞI TẠO & SINH GEN THÔNG MINH (QUAN TRỌNG) ---
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
        boolean isOnline = isOnlineCourse(off);
        String requiredType = getRequiredRoomType(off);

        // 1. Chọn ngày (Day)
        int day;
        if (isOnline) {
            // Online học tối -> Có thể học cả tuần (2-8)
            day = 2 + (int)(Math.random() * 7); 
        } else {
            // Offline ưu tiên học 2-7, hạn chế CN (trừ khi random trúng)
            // Tỷ lệ: 90% rơi vào 2-7, 10% rơi vào CN nếu cần
            if (Math.random() < 0.9) day = 2 + (int)(Math.random() * 6);
            else day = 8;
        }

        // 2. Chọn Kíp (Start Period) - TUÂN THỦ QUY CHẾ
        int[] validSlots;
        if (isOnline) {
            validSlots = new int[]{13}; // Bắt buộc tối
        } else {
            // Offline: 1, 4, 7, 10
            validSlots = new int[]{1, 4, 7, 10}; 
        }
        int start = validSlots[(int)(Math.random() * validSlots.length)];

        // 3. Chọn Phòng (Room) - Lọc đúng loại ngay từ đầu
        List<Room> candidates = rooms.stream()
                .filter(r -> r.getType().equalsIgnoreCase(requiredType))
                .filter(r -> r.getCapacity() >= off.getPlannedSize()) // Đủ chỗ
                .collect(Collectors.toList());

        // Fallback: Nếu không có phòng vừa vặn, lấy tạm phòng đúng loại (chấp nhận thiếu chỗ để phạt sau)
        if (candidates.isEmpty()) {
            candidates = rooms.stream()
                    .filter(r -> r.getType().equalsIgnoreCase(requiredType))
                    .collect(Collectors.toList());
        }
        // Fallback cuối cùng: Random đại (hiếm khi xảy ra nếu data chuẩn)
        if (candidates.isEmpty()) candidates = rooms;

        Room selectedRoom = candidates.get((int)(Math.random() * candidates.size()));

        return new Gene(day, start, selectedRoom);
    }

    // --- HÀM TÍNH ĐIỂM (FITNESS FUNCTION) ---
    private void calculateFitness(Schedule schedule, List<CourseOffering> allOfferings) {
        double score = 0;
        
        // Map kiểm tra trùng: Key -> OfferingID
        Map<String, Long> roomMap = new HashMap<>(); // "Day-Period-RoomID"
        Map<String, Long> lecMap = new HashMap<>();  // "Day-Period-LecID"
        Map<String, Long> classMap = new HashMap<>(); // "Day-Period-ClassCode"

        for (CourseOffering off : allOfferings) {
            Gene gene = schedule.genes.get(off.getId());
            
            int duration = (int)Math.ceil(off.getCourse().getCredits());
            if (duration <= 0) duration = 3;

            // 1. Phạt Vi phạm Loại phòng & Sức chứa (Dù randomGene đã lọc, nhưng mutation có thể gây lỗi)
            if (!gene.room.getType().equalsIgnoreCase(getRequiredRoomType(off))) score -= 1000;
            if (gene.room.getCapacity() < off.getPlannedSize()) score -= 500;

            // 2. Phạt Vi phạm Kíp chuẩn (Double check)
            boolean isOnline = isOnlineCourse(off);
            if (isOnline && gene.startPeriod != 13) score -= 1000;
            if (!isOnline && gene.startPeriod == 13) score -= 100; // Phạt nhẹ, tránh học tối nếu ko cần thiết

            // 3. Kiểm tra trùng lặp theo từng tiết
            for (int t = 0; t < duration; t++) {
                int period = gene.startPeriod + t;
                
                // Trùng Phòng (Trừ Online)
                if (!"ONLINE".equalsIgnoreCase(gene.room.getType())) {
                    String key = gene.day + "-" + period + "-" + gene.room.getId();
                    if (roomMap.containsKey(key)) score -= 1000;
                    else roomMap.put(key, off.getId());
                }

                // Trùng Giảng viên
                if (off.getLecturer() != null) {
                    String key = gene.day + "-" + period + "-" + off.getLecturer().getId();
                    if (lecMap.containsKey(key)) score -= 1000;
                    else lecMap.put(key, off.getId());
                }

                // Trùng Lớp biên chế (Sinh viên)
                if (off.getTargetClasses() != null) {
                    for (String cls : off.getTargetClasses().split(";")) {
                        String key = gene.day + "-" + period + "-" + cls.trim();
                        if (classMap.containsKey(key)) score -= 200; // Phạt nhẹ hơn trùng phòng
                        else classMap.put(key, off.getId());
                    }
                }
            }

            // 4. RÀNG BUỘC CHA - CON (QUAN TRỌNG NHẤT)
            if (off.getParent() != null) {
                Gene parentGene = schedule.genes.get(off.getParent().getId());
                if (parentGene != null && parentGene.day == gene.day) {
                    int pDuration = (int)Math.ceil(off.getParent().getCourse().getCredits());
                    int pEnd = parentGene.startPeriod + pDuration - 1;
                    int cEnd = gene.startPeriod + duration - 1;

                    // Nếu khoảng thời gian giao nhau
                    if (gene.startPeriod <= pEnd && cEnd >= parentGene.startPeriod) {
                        score -= 2000; // Phạt cực nặng để GA loại bỏ ngay
                    }
                }
            }
        }
        schedule.fitness = score;
        schedule.isFitnessChanged = false;
    }

    // --- EVOLUTION HELPERS ---
    private List<Schedule> evolvePopulation(List<Schedule> pop, List<CourseOffering> offerings, List<Room> rooms) {
        List<Schedule> newPop = new ArrayList<>();
        
        // Elitism: Giữ lại top 5% tốt nhất không qua lai ghép
        int eliteCount = (int)(POPULATION_SIZE * 0.05);
        for (int i = 0; i < eliteCount; i++) newPop.add(new Schedule(pop.get(i)));

        // Lai ghép & Đột biến
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
            // Lai ghép đồng nhất (Uniform Crossover): 50/50 gen từ bố/mẹ
            child.genes.put(off.getId(), Math.random() < 0.5 ? 
                new Gene(p1.genes.get(off.getId())) : new Gene(p2.genes.get(off.getId())));
        }
        return child;
    }

    private void mutate(Schedule child, List<CourseOffering> offerings, List<Room> rooms) {
        for (CourseOffering off : offerings) {
            if (Math.random() < MUTATION_RATE) {
                // Đột biến thông minh: Sinh lại gen hợp lệ
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
            
            if (best.fitness >= -100) off.setStatus("SCHEDULED");
            else off.setStatus("ERROR");
            
            off.setStatusMessage("GA Fitness: " + (int)best.fitness);
            toSave.add(off);
        }
        offeringRepo.saveAll(toSave);
    }

    // --- HELPER FUNCTIONS ---
    private boolean isOnlineCourse(CourseOffering off) {
        String type = off.getClassType() != null ? off.getClassType().toUpperCase() : "";
        String name = off.getCourse().getName().toUpperCase();
        return type.equals("ELN") || type.equals("COURSERA") 
            || name.contains("COURSERA") || Boolean.TRUE.equals(off.getCourse().getIsOnline());
    }

    private String getRequiredRoomType(CourseOffering off) {
        if (isOnlineCourse(off)) return "ONLINE";
        if ("TH".equalsIgnoreCase(off.getClassType())) return "LAB";
        return "THEORY";
    }
}