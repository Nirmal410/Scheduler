package com.assigment.Scheduler.service;

import com.assigment.Scheduler.dto.InterviewDTO;
import com.assigment.Scheduler.dto.ScheduleResultDTO;
import com.assigment.Scheduler.dto.UnscheduledDTO;
import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SchedulingService {

    private final ShortlistRepository shortlistRepository;
    private final InterviewRepository interviewRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PanelRepository panelRepository;
    private final RoomRepository roomRepository;
    private final ReplanLogRepository replanLogRepository;
    private final DisruptionRepository disruptionRepository;
    private final ResourceAvailabilityRepository availabilityRepository;
    private final CompanyRepository companyRepository;
    private final StudentRepository studentRepository;

    public SchedulingService(ShortlistRepository shortlistRepository,
            InterviewRepository interviewRepository,
            TimeSlotRepository timeSlotRepository,
            PanelRepository panelRepository,
            RoomRepository roomRepository,
            ReplanLogRepository replanLogRepository,
            DisruptionRepository disruptionRepository,
            ResourceAvailabilityRepository availabilityRepository,
            CompanyRepository companyRepository,
            StudentRepository studentRepository) {
        this.shortlistRepository = shortlistRepository;
        this.interviewRepository = interviewRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.panelRepository = panelRepository;
        this.roomRepository = roomRepository;
        this.replanLogRepository = replanLogRepository;
        this.disruptionRepository = disruptionRepository;
        this.availabilityRepository = availabilityRepository;
        this.companyRepository = companyRepository;
        this.studentRepository = studentRepository;
    }

    public static class ConflictIndex {
        public final Map<Long, Set<Long>> studentBusy = new HashMap<>();
        public final Map<Long, Set<Long>> roomBusy = new HashMap<>();
        public final Map<Long, Set<Long>> panelBusy = new HashMap<>();

        public boolean isStudentBusy(Long sid, Long tsid) {
            return studentBusy.getOrDefault(sid, Collections.emptySet()).contains(tsid);
        }

        public boolean isRoomBusy(Long rid, Long tsid) {
            return roomBusy.getOrDefault(rid, Collections.emptySet()).contains(tsid);
        }

        public boolean isPanelBusy(Long pid, Long tsid) {
            return panelBusy.getOrDefault(pid, Collections.emptySet()).contains(tsid);
        }

        public void markStudentBusy(Long sid, Long tsid) {
            studentBusy.computeIfAbsent(sid, k -> new HashSet<>()).add(tsid);
        }

        public void markRoomBusy(Long rid, Long tsid) {
            roomBusy.computeIfAbsent(rid, k -> new HashSet<>()).add(tsid);
        }

        public void markPanelBusy(Long pid, Long tsid) {
            panelBusy.computeIfAbsent(pid, k -> new HashSet<>()).add(tsid);
        }

        public void clearStudent(Long sid, Long tsid) {
            Set<Long> s = studentBusy.get(sid);
            if (s != null)
                s.remove(tsid);
        }

        public void clearRoom(Long rid, Long tsid) {
            Set<Long> s = roomBusy.get(rid);
            if (s != null)
                s.remove(tsid);
        }

        public void clearPanel(Long pid, Long tsid) {
            Set<Long> s = panelBusy.get(pid);
            if (s != null)
                s.remove(tsid);
        }
    }

    @Transactional
    public ScheduleResultDTO runInitialSchedule() {
        long start = System.currentTimeMillis();
        // Delete dependent tables first to avoid FK constraint violations
        replanLogRepository.deleteAllInBatch();
        disruptionRepository.deleteAllInBatch();
        interviewRepository.deleteAllInBatch();
        interviewRepository.flush();

        ensureCompanyShortlistsAndPanels();

        List<Shortlist> shortlists = shortlistRepository.findAll();
        Map<Long, List<Shortlist>> compShortlists = shortlists.stream()
                .collect(Collectors.groupingBy(sl -> sl.getCompany().getId()));
        Map<Long, Map<Long, Integer>> compStudentCgpaRank = new HashMap<>();
        for (Map.Entry<Long, List<Shortlist>> e : compShortlists.entrySet()) {
            List<Shortlist> sls = new ArrayList<>(e.getValue());
            sls.sort(Comparator.comparingDouble((Shortlist s) -> s.getStudent().getCgpa()).reversed());
            Map<Long, Integer> rankMap = new HashMap<>();
            for (int i = 0; i < sls.size(); i++)
                rankMap.put(sls.get(i).getStudent().getId(), i + 1);
            compStudentCgpaRank.put(e.getKey(), rankMap);
        }

        List<Interview> candidates = new ArrayList<>(shortlists.size());
        for (Shortlist sl : shortlists) {
            Company c = sl.getCompany();
            int tierW = c.getTier() == CompanyTier.DREAM ? 10 : c.getTier() == CompanyTier.CORE ? 7 : 4;
            double cgpa = sl.getStudent().getCgpa();
            int cgpaRank = compStudentCgpaRank.get(c.getId()).getOrDefault(sl.getStudent().getId(), 9999);
            double cgpaRankScore = Math.max(0,
                    10 - Math.min(10, cgpaRank / Math.max(1.0, compShortlists.get(c.getId()).size() / 10.0)));
            double slScore = Math.max(0, 10 - (sl.getPriorityRank() - 1));
            double score = tierW * 100 + cgpa * 10 + cgpaRankScore + slScore;
            Interview iv = new Interview(c, sl.getStudent(), Math.round(score * 100.0) / 100.0);
            candidates.add(iv);
        }
        candidates.sort(Comparator.comparingDouble(Interview::getPriorityScore).reversed());

        ensureDefaultTimeSlots();
        ensureDefaultRooms();

        List<TimeSlot> allSlots = timeSlotRepository.findAll();
        allSlots.sort(Comparator.comparingInt(TimeSlot::getDay).thenComparingInt(TimeSlot::getSlotNumber));
        Map<Integer, List<TimeSlot>> slotsByDay = allSlots.stream()
                .collect(Collectors.groupingBy(TimeSlot::getDay));

        List<Room> rooms = roomRepository.findAll().stream().filter(Room::getIsActive).collect(Collectors.toList());
        List<Panel> allPanels = panelRepository.findAll();
        Map<Long, List<Panel>> panelsByCompany = allPanels.stream()
                .collect(Collectors.groupingBy(p -> p.getCompany().getId()));

        // ── PRE-LOAD availability windows once — avoid DB queries inside the hot loop ──
        // Key: "ROOM:id" or "PANEL:id"  →  list of availability windows
        Map<String, List<ResourceAvailability>> availabilityCache = new HashMap<>();
        for (ResourceAvailability a : availabilityRepository.findAll()) {
            String key = a.getResourceType() + ":" + a.getResourceId();
            availabilityCache.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
        }

        // Pre-filter rooms that have no availability restrictions (empty list = always available).
        // For those that do have restrictions, we keep them but check against the cache.
        // This avoids the O(rooms × slots × candidates) DB hit.
        List<Room> activeRooms = rooms; // already filtered for isActive above

        ConflictIndex index = new ConflictIndex();

        long scheduled = 0;
        long unscheduled = 0;
        List<Interview> toSave = new ArrayList<>(candidates.size());

        for (Interview iv : candidates) {
            Company comp = iv.getCompany();
            Student stud = iv.getStudent();
            if (Boolean.TRUE.equals(stud.getWithdrawn())) {
                iv.setStatus(InterviewStatus.REPLAN_REQUIRED);
                iv.setUnscheduledReason(UnscheduledReason.NO_COMMON_SLOT);
                unscheduled++;
                toSave.add(iv);
                continue;
            }
            List<Panel> companyPanels = panelsByCompany.getOrDefault(comp.getId(), Collections.emptyList()).stream()
                    .filter(p -> availableResourceCached(availabilityCache, "PANEL", p.getId(),
                            comp.getArrivalDay(), comp.getArrivalTime(), comp.getAvailableUntil()))
                    .limit(Math.max(1, Optional.ofNullable(comp.getRequiredPanels()).orElse(1)))
                    .collect(Collectors.toList());
            if (companyPanels.isEmpty()) {
                Panel autoPanel = new Panel("Panel 1 (" + comp.getName() + ")", comp, "Interviewer 1, Interviewer 2");
                autoPanel.setMemberCount(2);
                autoPanel = panelRepository.saveAndFlush(autoPanel);
                companyPanels = List.of(autoPanel);
            }

            int earliestDay = comp.getArrivalDay();
            boolean placed = false;
            boolean hadStudentConflict = false;
            boolean hadRoomExhaust = false;
            boolean hadPanelConflict = false;

            outer: for (int day = earliestDay; day <= 4; day++) {
                List<TimeSlot> daySlots = slotsByDay.getOrDefault(day, Collections.emptyList());
                for (TimeSlot ts : daySlots) {
                    if (!withinCompanyWindow(comp, ts) || index.isStudentBusy(stud.getId(), ts.getId())) {
                        hadStudentConflict = true;
                        continue;
                    }
                    // Pre-filter rooms for this timeslot using the cache (no DB hit)
                    List<Room> availableRoomsForSlot = activeRooms.stream()
                            .filter(r -> availableResourceCached(availabilityCache, "ROOM", r.getId(),
                                    ts.getDay(), ts.getStartTime(), ts.getEndTime()))
                            .limit(Math.max(1, Optional.ofNullable(comp.getRequiredRooms()).orElse(1)))
                            .collect(Collectors.toList());
                    for (Panel p : companyPanels) {
                        if (index.isPanelBusy(p.getId(), ts.getId())) {
                            hadPanelConflict = true;
                            continue;
                        }
                        for (Room r : availableRoomsForSlot) {
                            if (index.isRoomBusy(r.getId(), ts.getId())) {
                                hadRoomExhaust = true;
                                continue;
                            }
                            iv.setRoom(r);
                            iv.setPanel(p);
                            iv.setTimeSlot(ts);
                            iv.setStatus(InterviewStatus.SCHEDULED);
                            iv.setUnscheduledReason(null);
                            index.markStudentBusy(stud.getId(), ts.getId());
                            index.markRoomBusy(r.getId(), ts.getId());
                            index.markPanelBusy(p.getId(), ts.getId());
                            placed = true;
                            scheduled++;
                            break outer;
                        }
                    }
                }
            }

            if (!placed) {
                iv.setStatus(InterviewStatus.REPLAN_REQUIRED);
                UnscheduledReason reason;
                if (hadRoomExhaust && !hadStudentConflict)
                    reason = UnscheduledReason.ROOM_EXHAUSTED;
                else if (hadStudentConflict && !hadPanelConflict)
                    reason = UnscheduledReason.STUDENT_SLOT_CONFLICT;
                else if (hadPanelConflict && !hadStudentConflict)
                    reason = UnscheduledReason.PANEL_UNAVAILABLE;
                else
                    reason = UnscheduledReason.NO_COMMON_SLOT;
                iv.setUnscheduledReason(reason);
                unscheduled++;
            }
            toSave.add(iv);
        }

        interviewRepository.saveAllAndFlush(toSave);
        long elapsed = System.currentTimeMillis() - start;
        return new ScheduleResultDTO(candidates.size(), scheduled, unscheduled, elapsed);
    }

    private boolean withinCompanyWindow(Company c, TimeSlot ts) {
        try {
            java.time.LocalTime start = java.time.LocalTime.parse(ts.getStartTime());
            java.time.LocalTime end = java.time.LocalTime.parse(ts.getEndTime());
            java.time.LocalTime allowedStart = java.time.LocalTime.parse(c.getArrivalTime());
            java.time.LocalTime allowedEnd = java.time.LocalTime.parse(c.getAvailableUntil());
            return !start.isBefore(allowedStart) && !end.isAfter(allowedEnd);
        } catch (Exception ignored) {
            return true;
        }
    }

    private void ensureDefaultTimeSlots() {
        if (timeSlotRepository.count() > 0) return;
        String[] starts = {"09:00","09:45","10:30","11:15","12:00","13:00","13:45","14:30","15:15","16:00","16:45","17:30","18:15","19:00","19:45","20:30"};
        String[] ends   = {"09:45","10:30","11:15","12:00","12:45","13:45","14:30","15:15","16:00","16:45","17:30","18:15","19:00","19:45","20:30","21:15"};
        List<TimeSlot> slots = new ArrayList<>();
        for (int day = 1; day <= 4; day++) {
            for (int s = 0; s < 16; s++) {
                slots.add(new TimeSlot(day, s + 1, starts[s], ends[s]));
            }
        }
        timeSlotRepository.saveAllAndFlush(slots);
    }

    private void ensureDefaultRooms() {
        if (roomRepository.count() > 0) return;
        List<Room> rooms = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Room r = new Room("R-" + (100 + i), "Main Block", 1);
            r.setIsActive(true);
            rooms.add(r);
        }
        roomRepository.saveAllAndFlush(rooms);
    }

    private void ensureCompanyShortlistsAndPanels() {
        List<Company> allCompanies = companyRepository.findAll();
        List<Student> allStudents = studentRepository.findAll();
        for (Company c : allCompanies) {
            int req = Math.max(1, Optional.ofNullable(c.getRequiredPanels()).orElse(1));
            List<Panel> pList = panelRepository.findByCompanyId(c.getId());
            if (pList.size() < req) {
                for (int i = pList.size() + 1; i <= req; i++) {
                    Panel p = new Panel("Panel " + i + " (" + c.getName() + ")", c, "Interviewer 1, Interviewer 2");
                    p.setMemberCount(2);
                    panelRepository.saveAndFlush(p);
                }
            }

            List<Shortlist> existingSl = shortlistRepository.findByCompanyId(c.getId());
            if (existingSl.isEmpty() && !allStudents.isEmpty()) {
                int rank = 1;
                for (Student s : allStudents) {
                    if (Boolean.TRUE.equals(s.getWithdrawn())) continue;
                    if (s.getCgpa() != null && s.getCgpa() >= (c.getCgpaCutoff() != null ? c.getCgpaCutoff() : 0.0)) {
                        shortlistRepository.save(new Shortlist(c, s, rank++));
                    }
                }
                shortlistRepository.flush();
            }
        }
    }

    /**
     * In-memory availability check using a pre-loaded cache.
     * Key format: "ROOM:id" or "PANEL:id"
     * Empty list means no restrictions = always available.
     */
    private boolean availableResourceCached(Map<String, List<ResourceAvailability>> cache,
            String type, Long id, int day, String start, String end) {
        List<ResourceAvailability> windows = cache.get(type + ":" + id);
        if (windows == null || windows.isEmpty()) return true;
        return windows.stream().anyMatch(a -> a.getDay().equals(day) && Boolean.TRUE.equals(a.getAvailable())
                && a.getStartTime().compareTo(start) <= 0 && a.getEndTime().compareTo(end) >= 0);
    }

    private boolean availableResource(String type, Long id, int day, String start, String end) {
        List<ResourceAvailability> windows = availabilityRepository.findByResourceTypeAndResourceId(type, id);
        if (windows.isEmpty())
            return true;
        return windows.stream().anyMatch(a -> a.getDay().equals(day) && Boolean.TRUE.equals(a.getAvailable())
                && a.getStartTime().compareTo(start) <= 0 && a.getEndTime().compareTo(end) >= 0);
    }

    public ConflictIndex rebuildIndexFromScheduled() {
        ConflictIndex idx = new ConflictIndex();
        for (Interview iv : interviewRepository.findAllActiveScheduled()) {
            if (iv.getTimeSlot() != null) {
                idx.markStudentBusy(iv.getStudent().getId(), iv.getTimeSlot().getId());
                if (iv.getRoom() != null)
                    idx.markRoomBusy(iv.getRoom().getId(), iv.getTimeSlot().getId());
                if (iv.getPanel() != null)
                    idx.markPanelBusy(iv.getPanel().getId(), iv.getTimeSlot().getId());
            }
        }
        return idx;
    }

    public List<InterviewDTO> getSchedule(Integer day, Long companyId, Long studentId, String status) {
        List<Interview> interviews = interviewRepository.findAll();
        return interviews.stream()
                .filter(iv -> {
                    if (day != null && (iv.getTimeSlot() == null || !iv.getTimeSlot().getDay().equals(day)))
                        return false;
                    if (companyId != null && !iv.getCompany().getId().equals(companyId))
                        return false;
                    if (studentId != null && !iv.getStudent().getId().equals(studentId))
                        return false;
                    if (status != null && !iv.getStatus().name().equals(status))
                        return false;
                    return true;
                })
                .sorted(Comparator
                        .<Interview, Integer>comparing(iv -> iv.getTimeSlot() == null ? 999 : iv.getTimeSlot().getDay())
                        .thenComparing(iv -> iv.getTimeSlot() == null ? 999 : iv.getTimeSlot().getSlotNumber())
                        .thenComparing(iv -> iv.getRoom() == null ? "" : iv.getRoom().getRoomNumber()))
                .map(this::toInterviewDTO)
                .collect(Collectors.toList());
    }

    public List<UnscheduledDTO> getUnscheduled() {
        return interviewRepository.findByStatusIn(List.of(InterviewStatus.UNSCHEDULED, InterviewStatus.REPLAN_REQUIRED,
            InterviewStatus.COORDINATOR_REVIEW)).stream()
                .sorted(Comparator.comparingDouble(Interview::getPriorityScore).reversed())
                .map(this::toUnscheduledDTO)
                .collect(Collectors.toList());
    }

    public InterviewDTO toInterviewDTO(Interview iv) {
        InterviewDTO d = new InterviewDTO();
        d.setInterviewId(iv.getId());
        d.setStudentId(iv.getStudent().getId());
        d.setStudentName(iv.getStudent().getName());
        d.setStudentCgpa(iv.getStudent().getCgpa());
        d.setCompanyId(iv.getCompany().getId());
        d.setCompanyName(iv.getCompany().getName());
        d.setCompanyTier(iv.getCompany().getTier().name());
        if (iv.getRoom() != null) {
            d.setRoomId(iv.getRoom().getId());
            d.setRoomNumber(iv.getRoom().getRoomNumber());
        }
        if (iv.getPanel() != null) {
            d.setPanelId(iv.getPanel().getId());
            d.setPanelName(iv.getPanel().getName());
        }
        if (iv.getTimeSlot() != null) {
            d.setTimeslotId(iv.getTimeSlot().getId());
            d.setDay(iv.getTimeSlot().getDay());
            d.setSlotNumber(iv.getTimeSlot().getSlotNumber());
            d.setStartTime(iv.getTimeSlot().getStartTime());
            d.setEndTime(iv.getTimeSlot().getEndTime());
        }
        d.setStatus(iv.getStatus().name());
        d.setPriorityScore(iv.getPriorityScore());
        if (iv.getUnscheduledReason() != null)
            d.setUnscheduledReason(iv.getUnscheduledReason().name());
        return d;
    }

    public UnscheduledDTO toUnscheduledDTO(Interview iv) {
        UnscheduledDTO u = new UnscheduledDTO();
        u.setInterviewId(iv.getId());
        u.setStudentId(iv.getStudent().getId());
        u.setStudentName(iv.getStudent().getName());
        u.setStudentCgpa(iv.getStudent().getCgpa());
        u.setCompanyId(iv.getCompany().getId());
        u.setCompanyName(iv.getCompany().getName());
        u.setCompanyTier(iv.getCompany().getTier().name());
        if (iv.getUnscheduledReason() != null) {
            u.setReasonCode(iv.getUnscheduledReason().name());
            u.setExplanation(explainReason(iv.getUnscheduledReason(), iv));
        }
        u.setStatus(iv.getStatus().name());
        return u;
    }

    private String explainReason(UnscheduledReason r, Interview iv) {
        switch (r) {
            case STUDENT_SLOT_CONFLICT:
                return "Student " + iv.getStudent().getName() + " already has interviews in all available slots for "
                        + iv.getCompany().getName() + " across the company's active days (Day "
                        + iv.getCompany().getArrivalDay() + "+).";
            case PANEL_UNAVAILABLE:
                return "No panel for " + iv.getCompany().getName()
                        + " was free in any slot compatible with student availability.";
            case ROOM_EXHAUSTED:
                return "All interview rooms were fully booked in candidate slots. More rooms or staggered arrival days are required.";
            case NO_COMMON_SLOT:
            default:
                return "No valid (Room + Panel + Student + TimeSlot) combination was found. Try increasing rooms or adjusting arrival days.";
        }
    }
}
