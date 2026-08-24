package com.assigment.Scheduler.service;

import com.assigment.Scheduler.dto.SeedRequest;
import com.assigment.Scheduler.dto.SeedResponse;
import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DataGeneratorService {

    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;
    private final RoomRepository roomRepository;
    private final PanelRepository panelRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ShortlistRepository shortlistRepository;
    private final InterviewRepository interviewRepository;
    private final DisruptionRepository disruptionRepository;
    private final ReplanLogRepository replanLogRepository;
    private final ResourceAvailabilityRepository resourceAvailabilityRepository;

    public DataGeneratorService(CompanyRepository companyRepository, StudentRepository studentRepository,
            RoomRepository roomRepository, PanelRepository panelRepository,
            TimeSlotRepository timeSlotRepository, ShortlistRepository shortlistRepository,
            InterviewRepository interviewRepository, DisruptionRepository disruptionRepository,
            ReplanLogRepository replanLogRepository, ResourceAvailabilityRepository resourceAvailabilityRepository) {
        this.companyRepository = companyRepository;
        this.studentRepository = studentRepository;
        this.roomRepository = roomRepository;
        this.panelRepository = panelRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.shortlistRepository = shortlistRepository;
        this.interviewRepository = interviewRepository;
        this.disruptionRepository = disruptionRepository;
        this.replanLogRepository = replanLogRepository;
        this.resourceAvailabilityRepository = resourceAvailabilityRepository;
    }

    private static final String[][] DREAM_COMPANIES = {
        {"Google India", "8.8", "1", "4"},
        {"Microsoft India Development Center", "8.7", "1", "4"},
        {"Amazon Development Center", "8.6", "1", "4"},
        {"Meta India", "8.9", "2", "3"},
        {"Apple India R&D", "8.9", "2", "2"},
        {"Tower Research Capital", "9.0", "2", "2"},
        {"Sprinklr", "8.5", "2", "3"}
    };

    private static final String[][] CORE_COMPANIES = {
        {"Zoho Corporation", "8.2", "1", "4"},
        {"Flipkart", "8.0", "1", "4"},
        {"Swiggy", "7.8", "2", "3"},
        {"Dream11 (Sporta Tech)", "8.0", "2", "3"},
        {"CRED", "8.1", "2", "3"},
        {"PhonePe", "7.9", "3", "3"},
        {"Razorpay", "7.8", "3", "3"},
        {"Udaan", "7.7", "3", "3"},
        {"MakeMyTrip", "7.6", "3", "3"},
        {"Paytm", "7.5", "3", "3"},
        {"Byju's (Think & Learn)", "7.5", "4", "3"},
        {"Ola Cabs (ANI Technologies)", "7.5", "4", "3"},
        {"Druva", "7.9", "4", "2"},
        {"Nutanix India", "8.0", "4", "2"}
    };

    private static final String[][] MASS_COMPANIES = {
        {"Tata Consultancy Services (TCS)", "6.0", "1", "5"},
        {"Infosys", "6.0", "1", "5"},
        {"Wipro", "6.0", "2", "5"},
        {"Cognizant", "6.0", "2", "5"},
        {"Accenture India", "6.0", "2", "5"},
        {"Capgemini India", "6.2", "3", "4"},
        {"Deloitte India", "7.0", "3", "3"},
        {"KPMG Global Services", "6.8", "3", "3"},
        {"EY GDS", "6.5", "4", "4"},
        {"PwC India", "6.8", "4", "3"},
        {"Tech Mahindra", "6.0", "4", "4"},
        {"Hexaware", "6.0", "4", "4"},
        {"LTIMindtree", "6.0", "4", "4"},
        {"DXC Technology", "6.0", "4", "3"},
        {"Atos India", "6.0", "4", "3"}
    };

    private static final String[] INDIAN_FIRST_NAMES = {
        "Aarav","Vivaan","Aditya","Vihaan","Arjun","Sai","Reyansh","Krishna","Ishaan","Shaurya",
        "Ananya","Diya","Aadhya","Saanvi","Myra","Aarohi","Anika","Arya","Kiara","Akshara",
        "Rohan","Rahul","Ritesh","Siddharth","Karan","Arnav","Ayush","Manan","Kabir","Dev",
        "Priya","Pooja","Riya","Sneha","Shreya","Nisha","Kavya","Tanya","Neha","Simran",
        "Vikram","Arun","Karthik","Nikhil","Akash","Ajay","Rajeev","Suresh","Ramesh","Mohit",
        "Anjali","Meera","Ishita","Riddhi","Tanvi","Khushi","Sanvi","Roshni","Ritu","Nandini",
        "Harsh","Parth","Shivam","Yash","Atharva","Abhinav","Pranav","Dhruv","Rudra","Om",
        "Anya","Sara","Jiya","Navya","Avni","Prisha","Aashi","Disha","Esha","Trisha"
    };

    private static final String[] INDIAN_LAST_NAMES = {
        "Sharma","Verma","Gupta","Singh","Kumar","Patel","Shah","Jain","Reddy","Iyer",
        "Menon","Nair","Das","Sen","Bose","Roy","Mukherjee","Chatterjee","Banerjee","Khan",
        "Pandey","Tiwari","Verma","Mishra","Chauhan","Rao","Chandra","Sekhar","Ravi","Prasad",
        "Krishnan","Narayanan","Srinivasan","Ramakrishnan","Subramanian","Shetty","Salvi","Desai","Gandhi","Thakkar",
        "Kapoor","Khanna","Agarwal","Saxena","Bhatia","Malhotra","Arora","Mehta","Chopra","Sethi"
    };

    private static final String[] BRANCHES = {
        "Computer Science & Engineering",
        "Information Technology",
        "Electronics & Communication Engineering",
        "Electrical & Electronics Engineering",
        "Mechanical Engineering",
        "Civil Engineering"
    };

    private static final double[] BRANCH_WEIGHTS = {0.28, 0.22, 0.18, 0.12, 0.12, 0.08};

    private static final String[] TIME_SLOT_STARTS = {
        "09:00","09:45","10:30","11:15","12:00","13:00","13:45","14:30",
        "15:15","16:00","16:45","17:30","18:15","19:00","19:45","20:30"
    };
    private static final String[] TIME_SLOT_ENDS = {
        "09:45","10:30","11:15","12:00","12:45","13:45","14:30","15:15",
        "16:00","16:45","17:30","18:15","19:00","19:45","20:30","21:15"
    };

    @Transactional
    public SeedResponse seedDatabase(SeedRequest request) {
        long start = System.currentTimeMillis();
        clearAllData();

        String scenario = request.getScenario() != null ? request.getScenario().toUpperCase() : "DEFAULT";

        int studentCount = request.getStudentCount() != null ? request.getStudentCount() : 800;
        int companyCount = request.getCompanyCount() != null ? request.getCompanyCount() : 35;
        int roomCount = request.getRoomCount() != null ? request.getRoomCount() : 14;
        long seed = request.getRandomSeed() != null ? request.getRandomSeed() : 42L;

        if ("HIGH_CONFLICT".equals(scenario)) {
            // Highly constrained bottleneck: 3 rooms for 30 companies, dense student shortlists
            roomCount = 3;
            companyCount = 30;
            studentCount = 300;
        } else if ("IMPOSSIBLE_REPLAN".equals(scenario) || "IMPOSSIBLE".equals(scenario)) {
            // Ultra bottlenecked dataset: 1 room total, all companies on Day 1, zero spare slots
            roomCount = 1;
            companyCount = 10;
            studentCount = 20;
        }

        Random random = new Random(seed);

        int companiesCreated = seedCompanies(companyCount, random, scenario);
        int studentsCreated = seedStudents(studentCount, random);
        int roomsCreated = seedRooms(roomCount);
        int timeSlotsCreated = seedTimeSlots();
        int panelsCreated = seedPanels(random, scenario);
        int shortlistsCreated = seedShortlists(random, scenario);

        long elapsed = System.currentTimeMillis() - start;
        return new SeedResponse(
            "SUCCESS", companiesCreated, studentsCreated, roomsCreated,
            panelsCreated, timeSlotsCreated, shortlistsCreated, elapsed
        );
    }

    @Transactional
    public void clearAllData() {
        // Order must respect FK constraints: children before parents.
        // 1. ReplanLog references Interview and Disruption
        replanLogRepository.deleteAllInBatch();
        replanLogRepository.flush();
        // 2. Disruption is now safe to delete
        disruptionRepository.deleteAllInBatch();
        disruptionRepository.flush();
        // 3. Interview references Company, Student, Room, Panel, TimeSlot
        interviewRepository.deleteAllInBatch();
        interviewRepository.flush();
        // 4. Shortlist references Company and Student
        shortlistRepository.deleteAllInBatch();
        shortlistRepository.flush();
        // 5. ResourceAvailability references rooms and panels by ID (logical FK)
        resourceAvailabilityRepository.deleteAllInBatch();
        resourceAvailabilityRepository.flush();
        // 6. Panel references Company
        panelRepository.deleteAllInBatch();
        panelRepository.flush();
        // 7. TimeSlot has no parents
        timeSlotRepository.deleteAllInBatch();
        timeSlotRepository.flush();
        // 8. Room has no parents
        roomRepository.deleteAllInBatch();
        roomRepository.flush();
        // 9. Student has no parents
        studentRepository.deleteAllInBatch();
        studentRepository.flush();
        // 10. Company is now safe (no children remaining)
        companyRepository.deleteAllInBatch();
        companyRepository.flush();
    }

    private int seedCompanies(int companyCount, Random random, String scenario) {
        List<Company> companies = new ArrayList<>();
        if ("IMPOSSIBLE_REPLAN".equals(scenario) || "IMPOSSIBLE".equals(scenario)) {
            // All companies arrive on Day 1 with 1 panel each to force slot saturation
            int total = Math.min(companyCount, DREAM_COMPANIES.length + CORE_COMPANIES.length);
            for (int i = 0; i < Math.min(total, DREAM_COMPANIES.length); i++) {
                String[] c = DREAM_COMPANIES[i];
                companies.add(new Company(c[0], CompanyTier.DREAM, Double.parseDouble(c[1]), 1, 1));
            }
            int remaining = total - companies.size();
            for (int i = 0; i < remaining; i++) {
                String[] c = CORE_COMPANIES[i];
                companies.add(new Company(c[0], CompanyTier.CORE, Double.parseDouble(c[1]), 1, 1));
            }
            companyRepository.saveAllAndFlush(companies);
            return companies.size();
        }

        int dreamCount = Math.min(DREAM_COMPANIES.length, (int) Math.ceil(companyCount * 0.20));
        int coreCount = Math.min(CORE_COMPANIES.length, (int) Math.ceil(companyCount * 0.40));
        int massCount = Math.min(MASS_COMPANIES.length, companyCount - dreamCount - coreCount);

        for (int i = 0; i < dreamCount; i++) {
            String[] c = DREAM_COMPANIES[i];
            companies.add(new Company(c[0], CompanyTier.DREAM, Double.parseDouble(c[1]),
                    Integer.parseInt(c[2]), Integer.parseInt(c[3])));
        }
        for (int i = 0; i < coreCount; i++) {
            String[] c = CORE_COMPANIES[i];
            companies.add(new Company(c[0], CompanyTier.CORE, Double.parseDouble(c[1]),
                    Integer.parseInt(c[2]), Integer.parseInt(c[3])));
        }
        for (int i = 0; i < massCount; i++) {
            String[] c = MASS_COMPANIES[i];
            companies.add(new Company(c[0], CompanyTier.MASS, Double.parseDouble(c[1]),
                    Integer.parseInt(c[2]), Integer.parseInt(c[3])));
        }
        companyRepository.saveAllAndFlush(companies);
        return companies.size();
    }

    private int seedStudents(int studentCount, Random random) {
        List<Student> students = new ArrayList<>(studentCount);
        for (int i = 0; i < studentCount; i++) {
            String firstName = INDIAN_FIRST_NAMES[random.nextInt(INDIAN_FIRST_NAMES.length)];
            String lastName = INDIAN_LAST_NAMES[random.nextInt(INDIAN_LAST_NAMES.length)];
            String name = firstName + " " + lastName;
            String email = (firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@student.edu").replaceAll("\\s+", "");

            double gaussian = random.nextGaussian();
            double cgpa = 7.2 + gaussian * 1.3;
            cgpa = Math.max(4.0, Math.min(10.0, Math.round(cgpa * 100.0) / 100.0));

            String branch = pickWeightedBranch(random);
            students.add(new Student(name, cgpa, branch, email));
        }
        studentRepository.saveAllAndFlush(students);
        return students.size();
    }

    private String pickWeightedBranch(Random random) {
        double r = random.nextDouble();
        double cumulative = 0;
        for (int i = 0; i < BRANCHES.length; i++) {
            cumulative += BRANCH_WEIGHTS[i];
            if (r <= cumulative) return BRANCHES[i];
        }
        return BRANCHES[0];
    }

    private int seedRooms(int roomCount) {
        List<Room> rooms = new ArrayList<>(roomCount);
        String[] buildings = {"A-Block", "B-Block", "C-Block", "D-Block"};
        for (int i = 1; i <= roomCount; i++) {
            String bld = buildings[(i - 1) % buildings.length];
            int floor = ((i - 1) / buildings.length) + 1;
            String roomNum = "R-" + floor + String.format("%02d", i);
            rooms.add(new Room(roomNum, bld, 1));
        }
        roomRepository.saveAllAndFlush(rooms);
        return rooms.size();
    }

    private int seedTimeSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        for (int day = 1; day <= 4; day++) {
            for (int s = 0; s < 16; s++) {
                slots.add(new TimeSlot(day, s + 1, TIME_SLOT_STARTS[s], TIME_SLOT_ENDS[s]));
            }
        }
        timeSlotRepository.saveAllAndFlush(slots);
        return slots.size();
    }

    private int seedPanels(Random random, String scenario) {
        List<Company> companies = companyRepository.findAll();
        List<Panel> panels = new ArrayList<>();
        String[] interviewerFirst = {"Rajesh","Suresh","Amit","Priya","Anjali","Vikram","Neha","Karthik","Deepa","Arjun","Meera","Rohan"};
        String[] interviewerLast = {"Patil","Rao","Sharma","Venkatesh","Iyer","Nair","Kapoor","Bose","Khan","Mehta","Singh","Gandhi"};
        for (Company comp : companies) {
            int panelsForCompany = comp.getMaxPanels();
            for (int p = 1; p <= panelsForCompany; p++) {
                int n1 = random.nextInt(interviewerFirst.length);
                int n2 = random.nextInt(interviewerFirst.length);
                int n3 = random.nextInt(interviewerFirst.length);
                while (n2 == n1) n2 = random.nextInt(interviewerFirst.length);
                while (n3 == n1 || n3 == n2) n3 = random.nextInt(interviewerFirst.length);
                String interviewers = interviewerFirst[n1] + " " + interviewerLast[(n1+n2)%interviewerLast.length] + ", "
                                    + interviewerFirst[n2] + " " + interviewerLast[(n2+n3)%interviewerLast.length] + ", "
                                    + interviewerFirst[n3] + " " + interviewerLast[(n3+n1)%interviewerLast.length];
                panels.add(new Panel(comp.getName() + " Panel " + p, comp, interviewers));
            }
        }
        panelRepository.saveAllAndFlush(panels);
        return panels.size();
    }

    private int seedShortlists(Random random, String scenario) {
        List<Company> companies = companyRepository.findAll();
        List<Student> students = studentRepository.findAll();
        Map<String, Double> branchBoost = Map.of(
            "Computer Science & Engineering", 1.0,
            "Information Technology", 0.92,
            "Electronics & Communication Engineering", 0.80,
            "Electrical & Electronics Engineering", 0.70,
            "Mechanical Engineering", 0.45,
            "Civil Engineering", 0.35
        );
        Map<Long, Integer> studentShortlistCount = new HashMap<>();
        Map<Long, List<Long>> companyShortlisted = new HashMap<>();
        for (Company c : companies) companyShortlisted.put(c.getId(), new ArrayList<>());
        List<Shortlist> allShortlists = new ArrayList<>();

        if ("IMPOSSIBLE_REPLAN".equals(scenario) || "IMPOSSIBLE".equals(scenario)) {
            // Create exactly 16 shortlists to 100% saturate all 16 slots of Room R-101 on Day 1.
            // When any disruption occurs, 0 alternate slots exist.
            int idx = 0;
            for (int s = 0; s < 16; s++) {
                Company c = companies.get(s % companies.size());
                Student st = students.get(idx % students.size());
                allShortlists.add(new Shortlist(c, st, (s / companies.size()) + 1));
                idx++;
            }
            shortlistRepository.saveAllAndFlush(allShortlists);
            return allShortlists.size();
        }

        boolean isHighConflict = "HIGH_CONFLICT".equals(scenario);
        int perCompanyBase = Math.max(5, (int) Math.round(students.size() * (isHighConflict ? 0.50 : 0.35)));
        for (Company comp : companies) {
            double tierMult = comp.getTier() == CompanyTier.DREAM ? 1.0
                            : comp.getTier() == CompanyTier.CORE ? 1.2 : 1.5;
            int targetShortlistSize = (int) Math.max(5, Math.min(students.size(), perCompanyBase * tierMult));
            List<Student> eligible = new ArrayList<>();
            for (Student s : students) {
                if (students.size() <= 30 || s.getCgpa() >= comp.getCgpaCutoff() - 0.3) {
                    double boost = branchBoost.getOrDefault(s.getBranch(), 0.5);
                    if (students.size() <= 30 || random.nextDouble() <= boost + 0.3) eligible.add(s);
                }
            }
            Collections.shuffle(eligible, random);
            int toTake = Math.min(targetShortlistSize, eligible.size());
            for (int i = 0; i < toTake; i++) {
                Student s = eligible.get(i);
                int count = studentShortlistCount.getOrDefault(s.getId(), 0);
                int maxShortlists = isHighConflict ? 12 : (comp.getTier() == CompanyTier.DREAM ? 6 :
                                    comp.getTier() == CompanyTier.CORE ? 5 : 4);
                if (count >= maxShortlists) continue;
                if (companyShortlisted.get(comp.getId()).contains(s.getId())) continue;
                Shortlist sl = new Shortlist(comp, s, i + 1);
                allShortlists.add(sl);
                companyShortlisted.get(comp.getId()).add(s.getId());
                studentShortlistCount.merge(s.getId(), 1, Integer::sum);
            }
        }

        for (Student s : students) {
            int count = studentShortlistCount.getOrDefault(s.getId(), 0);
            if (count == 0) {
                int tries = 0;
                while (count < 1 && tries < 20) {
                    Company c = companies.get(random.nextInt(companies.size()));
                    if (s.getCgpa() >= c.getCgpaCutoff() - 0.3 && !companyShortlisted.get(c.getId()).contains(s.getId())) {
                        int nextRank = companyShortlisted.get(c.getId()).size() + 1;
                        allShortlists.add(new Shortlist(c, s, nextRank));
                        companyShortlisted.get(c.getId()).add(s.getId());
                        count++;
                    }
                    tries++;
                }
            }
        }

        shortlistRepository.saveAllAndFlush(allShortlists);
        return allShortlists.size();
    }
}
