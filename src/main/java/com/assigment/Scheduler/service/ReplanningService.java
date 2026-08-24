package com.assigment.Scheduler.service;

import com.assigment.Scheduler.dto.*;
import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReplanningService {

    private static final int MAX_CASCADE_DEPTH = 3;

    // Repair score constants — same-day preferences
    private static final int SCORE_SAME_SLOT_DIFF_ROOM   = 100;
    private static final int SCORE_SAME_SLOT_DIFF_PANEL  = 100;
    private static final int SCORE_NEARBY_SLOT_SAME_DAY  = 80;
    private static final int SCORE_LATER_SLOT_SAME_DAY   = 60;
    private static final int SCORE_CASCADE_SAME_DAY      = 40;
    private static final int SCORE_CROSS_DAY             = 10;

    private final Map<String, ReplanState> previewSnapshots = new HashMap<>();

    private final DisruptionRepository disruptionRepository;
    private final InterviewRepository interviewRepository;
    private final ReplanLogRepository replanLogRepository;
    private final PanelRepository panelRepository;
    private final RoomRepository roomRepository;
    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final SchedulingService schedulingService;

    public ReplanningService(DisruptionRepository disruptionRepository,
            InterviewRepository interviewRepository, ReplanLogRepository replanLogRepository,
            PanelRepository panelRepository, RoomRepository roomRepository,
            StudentRepository studentRepository, CompanyRepository companyRepository,
            TimeSlotRepository timeSlotRepository, SchedulingService schedulingService) {
        this.disruptionRepository = disruptionRepository;
        this.interviewRepository = interviewRepository;
        this.replanLogRepository = replanLogRepository;
        this.panelRepository = panelRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.schedulingService = schedulingService;
    }

    // ── Inner types ──────────────────────────────────────────────────────────

    static class ReplanState {
        SchedulingService.ConflictIndex index;
        Map<Long, Interview> liveState;
        List<ReplanLogEntry> entries = new ArrayList<>();
        int cascadeMoves = 0;
        int cancelled = 0;
        int repaired = 0;
        int infeasible = 0;
        int totalScheduled = 0;
        Long exceptionInterviewId;

        // Same-day repair policy tracking
        int sameDayRepaired = 0;     // repaired within the disruption day
        int crossDayMoved = 0;       // repaired but moved to a different day
        int crossDayRequired = 0;    // could not be placed same-day; waiting for coordinator cross-day auth
        boolean allowCrossDay = false; // coordinator must explicitly enable cross-day movement
    }

    static class ReplanLogEntry {
        Interview interview;
        ReplanAction action;
        Long oldTs;
        Long oldRoom;
        Long oldPanel;
        Long newTs;
        Long newRoom;
        Long newPanel;
        int cascadeDepth;
        String reason;
        int repairScore; // higher = better same-day fit

        ReplanLogEntry(Interview iv, ReplanAction a, int depth, String reason) {
            this.interview = iv;
            this.action = a;
            this.cascadeDepth = depth;
            this.reason = reason;
        }
    }

    // ── Public API ───────────────────────────────────────────────────────────

    @Transactional
    public DisruptionResponse createDisruption(DisruptionRequest req) {
        Disruption d = new Disruption(req.getType(), req.getTargetEntityId(),
                req.getDay(), req.getStartSlot(), req.getEndSlot(), req.getReasonDescription());
        disruptionRepository.saveAndFlush(d);
        List<Interview> affected = findDirectlyAffected(d);
        return new DisruptionResponse(d.getId(), "LOGGED", affected.size(),
                d.getType().name(), d.getReasonDescription());
    }

    public List<DisruptionDTO> getAllDisruptions() {
        return disruptionRepository.findAll().stream()
                .sorted(Comparator.comparing(Disruption::getCreatedAt).reversed())
                .map(d -> {
                    DisruptionDTO dto = new DisruptionDTO();
                    dto.setDisruptionId(d.getId());
                    dto.setType(d.getType());
                    dto.setTargetEntityId(d.getTargetEntityId());
                    dto.setDay(d.getDay());
                    dto.setStartSlot(d.getStartSlot());
                    dto.setEndSlot(d.getEndSlot());
                    dto.setReasonDescription(d.getReasonDescription());
                    dto.setCreatedAt(d.getCreatedAt());
                    dto.setStatus(d.getStatus());
                    List<Interview> affected = findDirectlyAffected(d);
                    dto.setDirectlyAffectedCount(affected.size());
                    return dto;
                }).collect(Collectors.toList());
    }

    public ReplanDiffDTO previewReplan(Long disruptionId) {
        Optional<Disruption> opt = disruptionRepository.findById(disruptionId);
        if (opt.isEmpty()) {
            ReplanDiffDTO r = new ReplanDiffDTO();
            r.setStatus("DISRUPTION_NOT_FOUND");
            return r;
        }
        Disruption disruption = opt.get();
        ReplanState state = simulateReplan(disruption);
        String snapshotId = UUID.randomUUID().toString();
        state.totalScheduled = (int) interviewRepository.findAllActiveScheduled().size();
        previewSnapshots.put(snapshotId, state);
        ReplanDiffDTO dto = buildDiff(disruption, state);
        dto.setSnapshotId(snapshotId);
        // Status: REPLAN_REQUIRES_DECISION only when truly infeasible everywhere (not just cross-day required)
        boolean needsDecision = state.infeasible > 0 || state.crossDayRequired > 0;
        dto.setStatus(needsDecision ? "REPLAN_REQUIRES_DECISION" : "PREVIEW");
        return dto;
    }

    @Transactional
    public ReplanDiffDTO previewException(Long interviewId) {
        Optional<Interview> interview = interviewRepository.findById(interviewId);
        if (interview.isEmpty()) {
            ReplanDiffDTO response = new ReplanDiffDTO();
            response.setStatus("INTERVIEW_NOT_FOUND");
            return response;
        }
        Interview target = interview.get();
        Disruption disruption = new Disruption(DisruptionType.PANEL_UNAVAILABLE, -1L,
                target.getCompany().getArrivalDay(), 1, 16,
                "Alternative search for " + target.getStudent().getName() + " - " + target.getCompany().getName());
        disruption.setId(0L);

        ReplanState state = state0(disruption);
        state.exceptionInterviewId = interviewId;
        Interview exception = state.liveState.get(interviewId);
        exception.setStatus(InterviewStatus.SCHEDULED);

        repairSingleInterview(state, exception, disruption, 0);

        state.totalScheduled = (int) interviewRepository.findAllActiveScheduled().size();
        String snapshotId = UUID.randomUUID().toString();
        previewSnapshots.put(snapshotId, state);
        ReplanDiffDTO response = buildDiff(disruption, state);
        response.setSnapshotId(snapshotId);
        boolean needsDecision = state.infeasible > 0 || state.crossDayRequired > 0;
        response.setStatus(needsDecision ? "REPLAN_REQUIRES_DECISION" : "PREVIEW");
        return response;
    }

    @Transactional
    public ReplanConfirmResponse confirmReplan(Long disruptionId, String snapshotId, String optionId) {
        Disruption disruption = (disruptionId != null && disruptionId > 0)
                ? disruptionRepository.findById(disruptionId).orElse(null)
                : null;
        if (disruption == null) {
            disruption = new Disruption(DisruptionType.PANEL_UNAVAILABLE, -1L, 1, 1, 16, "Exception Repair");
            disruption = disruptionRepository.saveAndFlush(disruption);
        }
        ReplanConfirmResponse resp = new ReplanConfirmResponse();
        resp.setDisruptionId(disruption.getId());
        resp.setTimestamp(LocalDateTime.now());

        ReplanState state = snapshotId == null ? null : previewSnapshots.get(snapshotId);
        if (state == null) {
            resp.setStatus("SNAPSHOT_EXPIRED");
            return resp;
        }

        // SAME_DAY_COMMIT: commit only same-day repairs, leave cross-day-required as unresolved
        if ("SAME_DAY_COMMIT".equals(optionId)) {
            if (state.sameDayRepaired == 0 && state.cancelled == 0) {
                resp.setStatus("NOTHING_TO_COMMIT");
                return resp;
            }
            persistReplanFiltered(disruption, state, false);
            markDisruptionResolved(disruption, state.crossDayRequired == 0);
            previewSnapshots.remove(snapshotId);
            resp.setStatus(state.crossDayRequired > 0 ? "PARTIALLY_COMMITTED" : "COMMITTED");
            populateResponseCounts(resp, state, false);
            return resp;
        }

        // ALLOW_CROSS_DAY: coordinator explicitly authorizes cross-day movement
        // Re-run the simulation with allowCrossDay=true to resolve the remaining interviews
        if ("ALLOW_CROSS_DAY".equals(optionId)) {
            state.allowCrossDay = true;
            // Re-attempt repair for cross-day-required interviews
            repairedCrossDayRequired(disruption, state);
            persistReplanFiltered(disruption, state, true);
            markDisruptionResolved(disruption, true);
            previewSnapshots.remove(snapshotId);
            resp.setStatus("COMMITTED");
            populateResponseCounts(resp, state, true);
            return resp;
        }

        // MINIMUM_MOVEMENT (legacy / backward compat): only if no cross-day required
        if ("MINIMUM_MOVEMENT".equals(optionId)) {
            if (state.infeasible > 0) {
                resp.setStatus("REPLAN_REQUIRES_DECISION");
                return resp;
            }
            if (state.crossDayRequired > 0) {
                resp.setStatus("CROSS_DAY_AUTHORIZATION_REQUIRED");
                return resp;
            }
            persistReplanFiltered(disruption, state, true);
            markDisruptionResolved(disruption, true);
            previewSnapshots.remove(snapshotId);
            resp.setStatus("COMMITTED");
            populateResponseCounts(resp, state, true);
            return resp;
        }

        resp.setStatus("OPTION_NOT_FOUND");
        return resp;
    }

    // ── Core simulation ──────────────────────────────────────────────────────

    /**
     * Phase-based same-day repair simulation:
     * Phase 1: Same-day local repair (score-ordered)
     * Phase 2: Same-day cascade repair
     * Phase 3: Mark remaining as crossDayRequired (NOT auto-moved)
     * Cross-day repair only happens when allowCrossDay=true (coordinator approved)
     */
    private ReplanState simulateReplan(Disruption d) {
        ReplanState state = new ReplanState();
        state.index = schedulingService.rebuildIndexFromScheduled();
        state.liveState = new HashMap<>();
        for (Interview iv : interviewRepository.findAll()) {
            Interview copy = cloneInterview(iv);
            state.liveState.put(copy.getId(), copy);
        }

        List<Interview> directlyAffected = findDirectlyAffectedFromState(d, state);
        for (Interview iv : directlyAffected)
            unbindFromIndex(state, iv);

        for (Interview iv : directlyAffected) {
            // Student withdraw → always cancel, no repair needed
            if (d.getType() == DisruptionType.STUDENT_WITHDRAW
                    && d.getTargetEntityId().equals(iv.getStudent().getId())) {
                ReplanLogEntry e = new ReplanLogEntry(iv, ReplanAction.CANCELLED, 0,
                        "Student withdrew from placement process");
                applyCancellation(state, iv, e);
                continue;
            }
            repairSingleInterview(state, iv, d, 0);
        }
        return state;
    }

    /**
     * Attempt all repair phases for a single interview.
     * Order: same-day local → same-day cascade → mark as crossDayRequired.
     */
    private void repairSingleInterview(ReplanState state, Interview iv, Disruption d, int depth) {
        int preferredDay = (iv.getTimeSlot() != null) ? iv.getTimeSlot().getDay()
                : iv.getCompany().getArrivalDay();

        // Phase 1: Same-day local repair
        boolean repaired = tryLocalRepairSameDay(state, iv, d, depth, preferredDay);

        // Phase 2: Same-day cascade repair
        if (!repaired) {
            repaired = tryCascadeRepairSameDay(state, iv, d, depth, preferredDay);
        }

        if (repaired) {
            state.repaired++;
            state.sameDayRepaired++;
            return;
        }

        // Phase 3: Cross-day (only if coordinator has explicitly allowed it)
        if (state.allowCrossDay) {
            repaired = tryRepairAnyDay(state, iv, d, depth, preferredDay);
            if (repaired) {
                state.repaired++;
                state.crossDayMoved++;
                return;
            }
        }

        // Cannot repair — mark as requiring coordinator cross-day decision
        state.crossDayRequired++;
        state.infeasible++;
    }

    /**
     * After coordinator approves cross-day, re-attempt repair for all crossDayRequired interviews.
     */
    private void repairedCrossDayRequired(Disruption d, ReplanState state) {
        // Find interviews still in disrupted state (not moved/repaired yet)
        List<Interview> stillAffected = new ArrayList<>();
        for (Interview iv : state.liveState.values()) {
            if (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED) {
                // Check if it was part of the disruption and NOT yet repaired to a new slot
                // We identify them by checking if they are still in a forbidden slot
                if (isAffectedByDisruption(d, iv)) {
                    stillAffected.add(iv);
                }
            }
        }

        int previousCrossDay = state.crossDayRequired;
        state.crossDayRequired = 0;
        state.infeasible = state.infeasible - previousCrossDay; // reset count

        for (Interview iv : stillAffected) {
            unbindFromIndex(state, iv);
        }

        for (Interview iv : stillAffected) {
            int preferredDay = (iv.getTimeSlot() != null) ? iv.getTimeSlot().getDay()
                    : iv.getCompany().getArrivalDay();
            boolean repaired = tryRepairAnyDay(state, iv, d, 0, preferredDay);
            if (repaired) {
                state.repaired++;
                state.crossDayMoved++;
            } else {
                state.infeasible++;
                state.crossDayRequired++;
            }
        }
    }

    // ── Repair Phase 1: Same-day local repair (score-ordered) ────────────────

    /**
     * Tries to repair the interview using only slots on the SAME day as preferredDay.
     * Slot candidates are scored and tried in descending score order:
     *   - Same slot number, different room  → SCORE_SAME_SLOT_DIFF_ROOM (100)
     *   - Same slot number, different panel → SCORE_SAME_SLOT_DIFF_PANEL (100)
     *   - Nearby slot (≤2 away), same day  → SCORE_NEARBY_SLOT_SAME_DAY (80)
     *   - Later slot, same day             → SCORE_LATER_SLOT_SAME_DAY (60)
     */
    private boolean tryLocalRepairSameDay(ReplanState state, Interview iv, Disruption d, int depth, int preferredDay) {
        Company comp = iv.getCompany();
        List<Panel> companyPanels = panelRepository.findByCompanyId(comp.getId());
        List<Room> rooms = roomRepository.findAll().stream().filter(Room::getIsActive).collect(Collectors.toList());

        if (day(comp) > preferredDay) return false; // company not arrived yet on preferred day
        List<TimeSlot> daySlots = timeSlotRepository.findByDay(preferredDay);
        if (daySlots.isEmpty()) return false;

        daySlots.sort(Comparator.comparingInt(TimeSlot::getSlotNumber));
        int originalSlot = iv.getTimeSlot() != null ? iv.getTimeSlot().getSlotNumber() : -1;

        Long oldTs = iv.getTimeSlot() != null ? iv.getTimeSlot().getId() : null;
        Long oldRoom = iv.getRoom() != null ? iv.getRoom().getId() : null;
        Long oldPanel = iv.getPanel() != null ? iv.getPanel().getId() : null;

        // Build scored candidates
        record Candidate(TimeSlot ts, Panel panel, Room room, int score, String label) {}
        List<Candidate> candidates = new ArrayList<>();

        for (TimeSlot ts : daySlots) {
            if (state.index.isStudentBusy(iv.getStudent().getId(), ts.getId())) continue;
            if (isSlotForbidden(d, iv, null, null, ts)) continue;
            if (!withinCompanyWindow(comp, ts)) continue;

            int slotDiff = Math.abs(ts.getSlotNumber() - originalSlot);
            int baseScore;
            String label;
            if (originalSlot >= 0 && ts.getSlotNumber() == originalSlot) {
                baseScore = SCORE_SAME_SLOT_DIFF_ROOM;
                label = "Same slot, different resource";
            } else if (originalSlot >= 0 && slotDiff <= 2) {
                baseScore = SCORE_NEARBY_SLOT_SAME_DAY;
                label = "Nearby slot (±" + slotDiff + " slots), same day";
            } else {
                baseScore = SCORE_LATER_SLOT_SAME_DAY;
                label = "Later slot on same day (slot " + ts.getSlotNumber() + ")";
            }

            for (Panel p : companyPanels) {
                if (state.index.isPanelBusy(p.getId(), ts.getId())) continue;
                if (isSlotForbidden(d, iv, p, null, ts)) continue;
                for (Room r : rooms) {
                    if (isSlotForbidden(d, iv, p, r, ts)) continue;
                    if (state.index.isRoomBusy(r.getId(), ts.getId())) continue;
                    candidates.add(new Candidate(ts, p, r, baseScore, label));
                }
            }
        }

        // Sort by score descending (higher = preferred)
        candidates.sort(Comparator.comparingInt(Candidate::score).reversed());

        for (Candidate c : candidates) {
            ReplanLogEntry e = new ReplanLogEntry(iv, ReplanAction.MOVED, depth,
                    c.label() + " [score=" + c.score() + "] — same-day repair (day " + preferredDay + ")");
            e.oldTs = oldTs;
            e.oldRoom = oldRoom;
            e.oldPanel = oldPanel;
            e.newTs = c.ts().getId();
            e.newRoom = c.room().getId();
            e.newPanel = c.panel().getId();
            e.repairScore = c.score();
            bindInterview(state, iv, c.panel(), c.room(), c.ts(), e);
            if (depth > 0) state.cascadeMoves++;
            return true;
        }

        return false;
    }

    // ── Repair Phase 2: Same-day cascade repair ──────────────────────────────

    /**
     * Cascade repair restricted to the same day as preferredDay.
     * Tries to evict lower-priority interviews so the current one can fit.
     */
    private boolean tryCascadeRepairSameDay(ReplanState state, Interview iv, Disruption d, int depth, int preferredDay) {
        if (depth >= MAX_CASCADE_DEPTH) return false;
        Company comp = iv.getCompany();
        List<Panel> companyPanels = panelRepository.findByCompanyId(comp.getId());
        List<Room> rooms = roomRepository.findAll().stream().filter(Room::getIsActive).collect(Collectors.toList());

        if (day(comp) > preferredDay) return false;
        List<TimeSlot> daySlots = timeSlotRepository.findByDay(preferredDay);
        if (daySlots.isEmpty()) return false;
        daySlots.sort(Comparator.comparingInt(TimeSlot::getSlotNumber));

        Long oldTs = iv.getTimeSlot() != null ? iv.getTimeSlot().getId() : null;
        Long oldRoom = iv.getRoom() != null ? iv.getRoom().getId() : null;
        Long oldPanel = iv.getPanel() != null ? iv.getPanel().getId() : null;

        Map<Long, Interview> byId = state.liveState;

        for (TimeSlot ts : daySlots) {
            if (!withinCompanyWindow(comp, ts)) continue;
            if (isSlotForbidden(d, iv, null, null, ts)) continue;

            // Try displacing a lower-priority student occupying this slot
            if (state.index.isStudentBusy(iv.getStudent().getId(), ts.getId())) {
                Interview occupying = findInterviewInSlotByStudent(byId, iv.getStudent().getId(), ts.getId());
                if (occupying != null && occupying.getPriorityScore() < iv.getPriorityScore()
                        && !isProtectedByDisruption(d, occupying)) {
                    if (tryEvictAndRepairSameDay(state, iv, occupying, ts, d, depth, oldTs, oldRoom, oldPanel, preferredDay))
                        return true;
                }
            }

            for (Panel p : companyPanels) {
                if (isSlotForbidden(d, iv, p, null, ts)) continue;
                if (state.index.isPanelBusy(p.getId(), ts.getId())) {
                    Interview occupying = findInterviewInSlotByPanel(byId, p.getId(), ts.getId());
                    if (occupying != null && occupying.getPriorityScore() < iv.getPriorityScore()
                            && !isProtectedByDisruption(d, occupying)) {
                        if (tryEvictAndRepairSameDay(state, iv, occupying, p, null, ts, d, depth, oldTs, oldRoom, oldPanel, preferredDay))
                            return true;
                    }
                }

                for (Room r : rooms) {
                    if (isSlotForbidden(d, iv, p, r, ts)) continue;
                    if (state.index.isRoomBusy(r.getId(), ts.getId())) {
                        Interview occupying = findInterviewInSlotByRoom(byId, r.getId(), ts.getId());
                        if (occupying != null && occupying.getPriorityScore() < iv.getPriorityScore()
                                && !isProtectedByDisruption(d, occupying)) {
                            if (tryEvictAndRepairSameDay(state, iv, occupying, p, r, ts, d, depth, oldTs, oldRoom, oldPanel, preferredDay))
                                return true;
                        }
                    }
                    // Double-check all clear (race condition in candidate generation)
                    if (!state.index.isStudentBusy(iv.getStudent().getId(), ts.getId())
                            && !state.index.isPanelBusy(p.getId(), ts.getId())
                            && !state.index.isRoomBusy(r.getId(), ts.getId())) {
                        ReplanLogEntry e = new ReplanLogEntry(iv, ReplanAction.MOVED, depth,
                                "Cascade repair (depth " + depth + "), same day " + preferredDay + " [score=" + SCORE_CASCADE_SAME_DAY + "]");
                        e.oldTs = oldTs; e.oldRoom = oldRoom; e.oldPanel = oldPanel;
                        e.newTs = ts.getId(); e.newRoom = r.getId(); e.newPanel = p.getId();
                        e.repairScore = SCORE_CASCADE_SAME_DAY;
                        bindInterview(state, iv, p, r, ts, e);
                        if (depth > 0) state.cascadeMoves++;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Repair Phase 3: Cross-day (coordinator-authorized only) ─────────────

    /**
     * Cross-day repair. Only called when coordinator has explicitly authorized cross-day movement.
     * Tries all days after the preferred day, then days before (but after company arrival).
     */
    private boolean tryRepairAnyDay(ReplanState state, Interview iv, Disruption d, int depth, int preferredDay) {
        Company comp = iv.getCompany();
        List<Panel> companyPanels = panelRepository.findByCompanyId(comp.getId());
        List<Room> rooms = roomRepository.findAll().stream().filter(Room::getIsActive).collect(Collectors.toList());

        // Build day order: skip preferredDay (already tried), prefer nearby days
        List<Integer> dayOrder = new ArrayList<>(Arrays.asList(1, 2, 3, 4));
        dayOrder.remove(Integer.valueOf(preferredDay));
        dayOrder.sort(Comparator.comparingInt(day -> Math.abs(day - preferredDay)));

        Long oldTs = iv.getTimeSlot() != null ? iv.getTimeSlot().getId() : null;
        Long oldRoom = iv.getRoom() != null ? iv.getRoom().getId() : null;
        Long oldPanel = iv.getPanel() != null ? iv.getPanel().getId() : null;

        for (Integer day : dayOrder) {
            if (day < comp.getArrivalDay()) continue;
            List<TimeSlot> daySlots = timeSlotRepository.findByDay(day);
            daySlots.sort(Comparator.comparingInt(TimeSlot::getSlotNumber));
            for (TimeSlot ts : daySlots) {
                if (state.index.isStudentBusy(iv.getStudent().getId(), ts.getId())) continue;
                if (isSlotForbidden(d, iv, null, null, ts)) continue;
                if (!withinCompanyWindow(comp, ts)) continue;
                for (Panel p : companyPanels) {
                    if (state.index.isPanelBusy(p.getId(), ts.getId())) continue;
                    if (isSlotForbidden(d, iv, p, null, ts)) continue;
                    for (Room r : rooms) {
                        if (isSlotForbidden(d, iv, p, r, ts)) continue;
                        if (state.index.isRoomBusy(r.getId(), ts.getId())) continue;
                        ReplanLogEntry e = new ReplanLogEntry(iv, ReplanAction.MOVED, depth,
                                "Cross-day repair: coordinator authorized move to Day " + day
                                + " [score=" + SCORE_CROSS_DAY + "]");
                        e.oldTs = oldTs; e.oldRoom = oldRoom; e.oldPanel = oldPanel;
                        e.newTs = ts.getId(); e.newRoom = r.getId(); e.newPanel = p.getId();
                        e.repairScore = SCORE_CROSS_DAY;
                        bindInterview(state, iv, p, r, ts, e);
                        if (depth > 0) state.cascadeMoves++;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ── Evict helpers (same-day constrained) ────────────────────────────────

    private boolean tryEvictAndRepairSameDay(ReplanState state, Interview incoming, Interview occupying,
            Panel panel, Room room, TimeSlot ts, Disruption d, int depth,
            Long inOldTs, Long inOldRoom, Long inOldPanel, int preferredDay) {
        Panel p = panel != null ? panel
                : (incoming.getPanel() != null ? incoming.getPanel()
                        : (occupying.getPanel() != null ? occupying.getPanel()
                                : panelRepository.findByCompanyId(incoming.getCompany().getId()).get(0)));
        Room r = room != null ? room
                : (incoming.getRoom() != null ? incoming.getRoom()
                        : (occupying.getRoom() != null ? occupying.getRoom()
                                : roomRepository.findAll().get(0)));

        // 1. Unbind occupying from its current slot
        unbindFromIndex(state, occupying);

        // 2. Temporarily reserve/bind incoming in state.index so occupying cannot take the exact same spot!
        state.index.markStudentBusy(incoming.getStudent().getId(), ts.getId());
        state.index.markPanelBusy(p.getId(), ts.getId());
        state.index.markRoomBusy(r.getId(), ts.getId());

        // 3. Attempt repairing occupying into another available slot
        boolean ok = tryLocalRepairSameDay(state, occupying, d, depth + 1, preferredDay);
        if (!ok) ok = tryCascadeRepairSameDay(state, occupying, d, depth + 1, preferredDay);

        if (!ok) {
            // Unreserve incoming
            state.index.clearStudent(incoming.getStudent().getId(), ts.getId());
            state.index.clearPanel(p.getId(), ts.getId());
            state.index.clearRoom(r.getId(), ts.getId());

            // Restore occupying back to its original slot
            Panel occP = occupying.getPanel();
            Room occR = occupying.getRoom();
            TimeSlot occTs = occupying.getTimeSlot();
            if (occTs != null) {
                state.index.markStudentBusy(occupying.getStudent().getId(), occTs.getId());
                if (occR != null) state.index.markRoomBusy(occR.getId(), occTs.getId());
                if (occP != null) state.index.markPanelBusy(occP.getId(), occTs.getId());
            }
            return false;
        }

        // 4. Finalize incoming's binding & log entry
        ReplanLogEntry e = new ReplanLogEntry(incoming, ReplanAction.MOVED, depth,
                "Cascade eviction (depth " + depth + "): displaced lower-priority interview, same-day repair [score=" + SCORE_CASCADE_SAME_DAY + "]");
        e.oldTs = inOldTs; e.oldRoom = inOldRoom; e.oldPanel = inOldPanel;
        e.newTs = ts.getId(); e.newRoom = r.getId(); e.newPanel = p.getId();
        e.repairScore = SCORE_CASCADE_SAME_DAY;

        state.entries.add(e);
        Interview live = state.liveState.get(incoming.getId());
        live.setPanel(p);
        live.setRoom(r);
        live.setTimeSlot(ts);
        live.setStatus(InterviewStatus.MOVED);
        live.setUnscheduledReason(null);

        state.cascadeMoves++;
        return true;
    }

    private boolean tryEvictAndRepairSameDay(ReplanState state, Interview incoming, Interview occupying,
            TimeSlot ts, Disruption d, int depth, Long inOldTs, Long inOldRoom, Long inOldPanel, int preferredDay) {
        return tryEvictAndRepairSameDay(state, incoming, occupying, null, null, ts, d, depth,
                inOldTs, inOldRoom, inOldPanel, preferredDay);
    }

    // ── Index helpers ────────────────────────────────────────────────────────

    private void unbindFromIndex(ReplanState state, Interview iv) {
        if (iv.getTimeSlot() != null) {
            state.index.clearStudent(iv.getStudent().getId(), iv.getTimeSlot().getId());
            if (iv.getRoom() != null)
                state.index.clearRoom(iv.getRoom().getId(), iv.getTimeSlot().getId());
            if (iv.getPanel() != null)
                state.index.clearPanel(iv.getPanel().getId(), iv.getTimeSlot().getId());
        }
    }

    private void bindInterview(ReplanState state, Interview iv, Panel p, Room r, TimeSlot ts, ReplanLogEntry e) {
        state.entries.add(e);
        Interview live = state.liveState.get(iv.getId());
        live.setPanel(p);
        live.setRoom(r);
        live.setTimeSlot(ts);
        live.setStatus(InterviewStatus.MOVED);
        live.setUnscheduledReason(null);
        state.index.markStudentBusy(live.getStudent().getId(), ts.getId());
        state.index.markRoomBusy(r.getId(), ts.getId());
        state.index.markPanelBusy(p.getId(), ts.getId());
    }

    private void applyCancellation(ReplanState state, Interview iv, ReplanLogEntry e) {
        e.oldTs = iv.getTimeSlot() != null ? iv.getTimeSlot().getId() : null;
        e.oldRoom = iv.getRoom() != null ? iv.getRoom().getId() : null;
        e.oldPanel = iv.getPanel() != null ? iv.getPanel().getId() : null;
        state.entries.add(e);
        Interview live = state.liveState.get(iv.getId());
        live.setStatus(InterviewStatus.CANCELLED);
        live.setTimeSlot(null);
        live.setRoom(null);
        live.setPanel(null);
        live.setUnscheduledReason(UnscheduledReason.NO_COMMON_SLOT);
        state.cancelled++;
    }

    // ── Diff builder ─────────────────────────────────────────────────────────

    private ReplanDiffDTO buildDiff(Disruption disruption, ReplanState state) {
        ReplanDiffDTO dto = new ReplanDiffDTO();
        dto.setDisruptionId(disruption.getId());
        int directlyAffected = state.exceptionInterviewId != null ? 1
            : findDirectlyAffectedFromState(disruption, state0(disruption)).size();
        dto.setDirectlyAffectedCount(directlyAffected);
        dto.setRepairedCount(state.repaired);
        dto.setCascadeMovesCount(state.cascadeMoves);
        dto.setCancelledCount(state.cancelled);

        long moved = state.entries.stream().filter(e -> e.action == ReplanAction.MOVED)
                .map(e -> e.interview.getId()).distinct().count();
        long critical = state.entries.stream().filter(e -> e.action == ReplanAction.MOVED)
                .filter(this::isCriticalMovement).count();
        dto.setTotalMovedCount((int) moved);
        dto.setTotalScheduledCount(state.totalScheduled);
        dto.setCriticalMovesCount((int) critical);
        dto.setCascadeRatio(
                directlyAffected == 0 ? 0.0 : Math.round(state.cascadeMoves * 100.0 / directlyAffected) / 100.0);

        double churnPercent = state.totalScheduled == 0 ? 0.0 : moved * 100.0 / state.totalScheduled;
        String band = churnPercent < 5.0 ? "GREEN" : churnPercent <= 10.0 ? "AMBER" : "RED";
        dto.setBudgetBand(band);

        boolean needsDecision = state.infeasible > 0 || state.crossDayRequired > 0;
        dto.setRequiresApproval(!("GREEN".equals(band)) || needsDecision);
        dto.setHardConstraintsValid(state.infeasible == 0);
        dto.setInfeasible(state.infeasible > 0 && state.crossDayRequired == state.infeasible
                ? false  // cross-day-only infeasibility → not truly infeasible, just needs authorization
                : state.infeasible > state.crossDayRequired);

        // ── Same-day status (GREEN/AMBER/RED) ─────────────────────────────
        int sameDayFixed = state.sameDayRepaired + state.cancelled; // cancelled = resolved
        int crossNeeded = state.crossDayRequired;
        int totalAffected = directlyAffected;

        String sameDayStatus;
        String sameDayMessage;
        if (crossNeeded == 0) {
            sameDayStatus = "GREEN";
            sameDayMessage = sameDayFixed + " of " + totalAffected
                    + " interviews repaired within Day " + disruption.getDay()
                    + ". No cross-day movement required.";
        } else if (sameDayFixed > 0 && crossNeeded < totalAffected) {
            sameDayStatus = "AMBER";
            sameDayMessage = sameDayFixed + " of " + totalAffected
                    + " interviews repaired on Day " + disruption.getDay()
                    + ". " + crossNeeded + " require coordinator approval for cross-day movement.";
        } else {
            sameDayStatus = "RED";
            sameDayMessage = "Same-day capacity exhausted. " + crossNeeded
                    + " of " + totalAffected
                    + " interviews cannot be repaired on Day " + disruption.getDay()
                    + ". Coordinator decision required.";
        }

        dto.setSameDayStatus(sameDayStatus);
        dto.setSameDayMessage(sameDayMessage);
        dto.setSameDayRepairedCount(state.sameDayRepaired);
        dto.setCrossDayMovedCount(state.crossDayMoved);
        dto.setCrossDayRequiredCount(crossNeeded);

        dto.setDecisionMessage(crossNeeded > 0
                ? "Same-day repair applied to " + sameDayFixed + " interviews. "
                  + crossNeeded + " interview(s) require cross-day authorization from the coordinator."
                : (state.infeasible > 0
                        ? "No feasible repair found without violating hard constraints. Coordinator decision required."
                        : "Review the ranked repair option and approve the exact preview snapshot before committing."));

        // Recommended option depends on same-day status
        if (crossNeeded > 0) {
            dto.setRecommendedOptionId("SAME_DAY_COMMIT");
        } else if (state.infeasible > 0) {
            dto.setRecommendedOptionId(null);
        } else {
            dto.setRecommendedOptionId("MINIMUM_MOVEMENT");
        }

        dto.setChurnRatio(directlyAffected == 0 ? 0.0
                : Math.round((state.cascadeMoves
                        + state.entries.stream().filter(e -> e.action == ReplanAction.MOVED).count())
                        * 100.0 / directlyAffected) / 100.0);

        // Build moved interviews list
        List<MovedInterviewDTO> list = buildMovedList(state);
        dto.setMovedInterviews(list);

        // Build options
        List<ReplanOptionDTO> options = new ArrayList<>();
        if (crossNeeded > 0) {
            // Option 1: commit today's repairs
            ReplanOptionDTO opt1 = new ReplanOptionDTO();
            opt1.setOptionId("SAME_DAY_COMMIT");
            opt1.setRank(1);
            opt1.setStrategy("SAME_DAY_COMMIT");
            opt1.setLabel("Commit " + sameDayFixed + " Same-Day Repairs");
            opt1.setRecommended(true);
            opt1.setTotalMoved((int) moved);
            opt1.setCascadeMoves(state.cascadeMoves);
            opt1.setCancelled(state.cancelled);
            opt1.setChurnRatio(dto.getChurnRatio());
            opt1.setBudgetBand(band);
            opt1.setRequiresApproval(false);
            opt1.setMovedInterviews(list);
            options.add(opt1);
            // Option 2: allow cross-day
            ReplanOptionDTO opt2 = new ReplanOptionDTO();
            opt2.setOptionId("ALLOW_CROSS_DAY");
            opt2.setRank(2);
            opt2.setStrategy("ALLOW_CROSS_DAY");
            opt2.setLabel("Authorize Cross-Day Movement for " + crossNeeded + " Interview(s)");
            opt2.setRecommended(false);
            opt2.setTotalMoved((int) moved + crossNeeded);
            opt2.setCascadeMoves(state.cascadeMoves);
            opt2.setCancelled(state.cancelled);
            opt2.setChurnRatio(dto.getChurnRatio());
            opt2.setBudgetBand("AMBER");
            opt2.setRequiresApproval(true);
            opt2.setMovedInterviews(list);
            options.add(opt2);
        } else if (state.infeasible == 0) {
            ReplanOptionDTO option = new ReplanOptionDTO();
            option.setOptionId("MINIMUM_MOVEMENT");
            option.setRank(1);
            option.setStrategy("MINIMUM_MOVEMENT");
            option.setLabel("Minimum Movement (Same-Day)");
            option.setRecommended(true);
            option.setTotalMoved((int) moved);
            option.setCascadeMoves(state.cascadeMoves);
            option.setCancelled(state.cancelled);
            option.setChurnRatio(dto.getChurnRatio());
            option.setBudgetBand(band);
            option.setRequiresApproval(dto.isRequiresApproval());
            option.setMovedInterviews(list);
            options.add(option);
        }
        dto.setOptions(options);
        return dto;
    }

    private List<MovedInterviewDTO> buildMovedList(ReplanState state) {
        Map<Long, TimeSlot> tsById = timeSlotRepository.findAll().stream()
                .collect(Collectors.toMap(TimeSlot::getId, t -> t));
        Map<Long, Room> rById = roomRepository.findAll().stream()
                .collect(Collectors.toMap(Room::getId, r -> r));
        Map<Long, Panel> pById = panelRepository.findAll().stream()
                .collect(Collectors.toMap(Panel::getId, p -> p));

        List<MovedInterviewDTO> list = new ArrayList<>();
        for (ReplanLogEntry e : state.entries) {
            MovedInterviewDTO m = new MovedInterviewDTO();
            m.setInterviewId(e.interview.getId());
            m.setStudentName(e.interview.getStudent().getName());
            m.setCompanyName(e.interview.getCompany().getName());
            if (e.oldTs != null) {
                TimeSlot t = tsById.get(e.oldTs);
                if (t != null) { m.setOldDay(t.getDay()); m.setOldSlot(t.getSlotNumber()); }
                if (e.oldRoom != null && rById.containsKey(e.oldRoom))
                    m.setOldRoom(rById.get(e.oldRoom).getRoomNumber());
                if (e.oldPanel != null && pById.containsKey(e.oldPanel))
                    m.setOldPanel(pById.get(e.oldPanel).getName());
            }
            if (e.newTs != null) {
                TimeSlot t = tsById.get(e.newTs);
                if (t != null) { m.setNewDay(t.getDay()); m.setNewSlot(t.getSlotNumber()); }
                if (e.newRoom != null && rById.containsKey(e.newRoom))
                    m.setNewRoom(rById.get(e.newRoom).getRoomNumber());
                if (e.newPanel != null && pById.containsKey(e.newPanel))
                    m.setNewPanel(pById.get(e.newPanel).getName());
            }
            m.setAction(e.action.name());
            m.setCascadeDepth(e.cascadeDepth);
            m.setReason(e.reason);
            list.add(m);
        }
        return list;
    }

    // ── Constraint helpers ───────────────────────────────────────────────────

    private boolean isProtectedByDisruption(Disruption d, Interview iv) {
        switch (d.getType()) {
            case ROOM_UNAVAILABLE:
                return iv.getRoom() != null && d.getTargetEntityId().equals(iv.getRoom().getId());
            case PANEL_UNAVAILABLE:
                return iv.getPanel() != null && d.getTargetEntityId().equals(iv.getPanel().getId());
            case COMPANY_LATE:
                return iv.getCompany().getId().equals(d.getTargetEntityId());
            default:
                return false;
        }
    }

    private boolean isAffectedByDisruption(Disruption d, Interview iv) {
        if (iv.getTimeSlot() == null) return false;
        switch (d.getType()) {
            case ROOM_UNAVAILABLE:
                return iv.getRoom() != null && iv.getRoom().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case PANEL_UNAVAILABLE:
                return iv.getPanel() != null && iv.getPanel().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case COMPANY_LATE:
                return iv.getCompany().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case STUDENT_WITHDRAW:
                return iv.getStudent().getId().equals(d.getTargetEntityId());
            default:
                return false;
        }
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

    private boolean isSlotForbidden(Disruption d, Interview iv, Panel p, Room r, TimeSlot ts) {
        if (!withinCompanyWindow(iv.getCompany(), ts)) return true;
        if (d.getType() == DisruptionType.COMPANY_LATE && iv.getCompany().getId().equals(d.getTargetEntityId())) {
            if (ts.getDay().equals(d.getDay()) && ts.getSlotNumber() >= d.getStartSlot()
                    && ts.getSlotNumber() <= d.getEndSlot()) return true;
        }
        if (d.getType() == DisruptionType.PANEL_UNAVAILABLE && p != null && d.getTargetEntityId().equals(p.getId())) {
            if (ts.getDay().equals(d.getDay()) && ts.getSlotNumber() >= d.getStartSlot()
                    && ts.getSlotNumber() <= d.getEndSlot()) return true;
        }
        if (d.getType() == DisruptionType.ROOM_UNAVAILABLE && r != null && d.getTargetEntityId().equals(r.getId())) {
            if (ts.getDay().equals(d.getDay()) && ts.getSlotNumber() >= d.getStartSlot()
                    && ts.getSlotNumber() <= d.getEndSlot()) return true;
        }
        if (d.getType() == DisruptionType.STUDENT_WITHDRAW && iv.getStudent().getId().equals(d.getTargetEntityId()))
            return true;
        return false;
    }

    // ── Slot finders ─────────────────────────────────────────────────────────

    private Interview findInterviewInSlotByStudent(Map<Long, Interview> byId, Long sid, Long tsid) {
        for (Interview iv : byId.values()) {
            if (iv.getTimeSlot() != null && iv.getStudent().getId().equals(sid)
                    && iv.getTimeSlot().getId().equals(tsid)
                    && (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED))
                return iv;
        }
        return null;
    }

    private Interview findInterviewInSlotByPanel(Map<Long, Interview> byId, Long pid, Long tsid) {
        for (Interview iv : byId.values()) {
            if (iv.getTimeSlot() != null && iv.getPanel() != null && iv.getPanel().getId().equals(pid)
                    && iv.getTimeSlot().getId().equals(tsid)
                    && (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED))
                return iv;
        }
        return null;
    }

    private Interview findInterviewInSlotByRoom(Map<Long, Interview> byId, Long rid, Long tsid) {
        for (Interview iv : byId.values()) {
            if (iv.getTimeSlot() != null && iv.getRoom() != null && iv.getRoom().getId().equals(rid)
                    && iv.getTimeSlot().getId().equals(tsid)
                    && (iv.getStatus() == InterviewStatus.SCHEDULED || iv.getStatus() == InterviewStatus.MOVED))
                return iv;
        }
        return null;
    }

    // ── Affected interview finders ───────────────────────────────────────────

    public List<Interview> findDirectlyAffected(Disruption d) {
        List<Interview> active = interviewRepository.findAllActiveScheduled();
        List<Interview> affected = new ArrayList<>();
        for (Interview iv : active) {
            if (matchesDisruption(d, iv)) affected.add(iv);
        }
        return affected;
    }

    private List<Interview> findDirectlyAffectedFromState(Disruption d, ReplanState state) {
        List<Interview> affected = new ArrayList<>();
        for (Interview iv : state.liveState.values()) {
            if (iv.getStatus() != InterviewStatus.SCHEDULED && iv.getStatus() != InterviewStatus.MOVED)
                continue;
            if (matchesDisruption(d, iv)) affected.add(iv);
        }
        return affected;
    }

    private boolean matchesDisruption(Disruption d, Interview iv) {
        switch (d.getType()) {
            case ROOM_UNAVAILABLE:
                return iv.getRoom() != null && iv.getTimeSlot() != null
                        && iv.getRoom().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case PANEL_UNAVAILABLE:
                return iv.getPanel() != null && iv.getTimeSlot() != null
                        && iv.getPanel().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case COMPANY_LATE:
                return iv.getCompany().getId().equals(d.getTargetEntityId())
                        && iv.getTimeSlot() != null
                        && iv.getTimeSlot().getDay().equals(d.getDay())
                        && iv.getTimeSlot().getSlotNumber() >= d.getStartSlot()
                        && iv.getTimeSlot().getSlotNumber() <= d.getEndSlot();
            case STUDENT_WITHDRAW:
                return iv.getStudent().getId().equals(d.getTargetEntityId());
            default:
                return false;
        }
    }

    // ── Persist helpers ──────────────────────────────────────────────────────

    /**
     * Persist replan. When includeCrossDay=false, skip interviews that were moved cross-day
     * (crossDayRequired cases); they remain in their original state pending coordinator decision.
     */
    private void persistReplanFiltered(Disruption d, ReplanState state, boolean includeCrossDay) {
        List<Interview> merged = new ArrayList<>();
        Set<Long> crossDayEntryIds = new HashSet<>();

        if (!includeCrossDay) {
            // Identify entries that are cross-day moves (score == SCORE_CROSS_DAY)
            for (ReplanLogEntry e : state.entries) {
                if (e.repairScore == SCORE_CROSS_DAY) {
                    crossDayEntryIds.add(e.interview.getId());
                }
            }
        }

        for (Interview live : state.liveState.values()) {
            if (!includeCrossDay && crossDayEntryIds.contains(live.getId())) continue;
            Interview db = interviewRepository.findById(live.getId()).orElse(null);
            if (db == null) continue;
            db.setStatus(live.getStatus());
            db.setRoom(live.getRoom());
            db.setPanel(live.getPanel());
            db.setTimeSlot(live.getTimeSlot());
            db.setUnscheduledReason(live.getUnscheduledReason());
            merged.add(db);
        }
        interviewRepository.saveAllAndFlush(merged);

        List<ReplanLog> logs = new ArrayList<>();
        for (ReplanLogEntry e : state.entries) {
            if (!includeCrossDay && crossDayEntryIds.contains(e.interview.getId())) continue;
            ReplanLog log = new ReplanLog(d, interviewRepository.getById(e.interview.getId()),
                    e.action, e.cascadeDepth, e.reason);
            log.setOldTimeslotId(e.oldTs);
            log.setOldRoomId(e.oldRoom);
            log.setOldPanelId(e.oldPanel);
            log.setNewTimeslotId(e.newTs);
            log.setNewRoomId(e.newRoom);
            log.setNewPanelId(e.newPanel);
            logs.add(log);
        }
        replanLogRepository.saveAllAndFlush(logs);
    }

    private void markDisruptionResolved(Disruption disruption, boolean fullyResolved) {
        if (disruption.getId() != null && disruption.getId() > 0
                && disruptionRepository.existsById(disruption.getId())) {
            disruption.setStatus(fullyResolved ? "RESOLVED" : "PARTIALLY_RESOLVED");
            disruptionRepository.save(disruption);
        }
    }

    private void populateResponseCounts(ReplanConfirmResponse resp, ReplanState state, boolean includeCrossDay) {
        for (ReplanLogEntry e : state.entries) {
            if (!includeCrossDay && e.repairScore == SCORE_CROSS_DAY) continue;
            if (e.action == ReplanAction.MOVED)
                resp.setInterviewsMoved(resp.getInterviewsMoved() + 1);
            else if (e.action == ReplanAction.CANCELLED)
                resp.setInterviewsCancelled(resp.getInterviewsCancelled() + 1);
            else if (e.action == ReplanAction.SCHEDULED)
                resp.setInterviewsNewlyScheduled(resp.getInterviewsNewlyScheduled() + 1);
        }
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    private Interview cloneInterview(Interview iv) {
        Interview c = new Interview(iv.getCompany(), iv.getStudent(), iv.getPriorityScore());
        c.setId(iv.getId());
        c.setStatus(iv.getStatus());
        c.setRoom(iv.getRoom());
        c.setPanel(iv.getPanel());
        c.setTimeSlot(iv.getTimeSlot());
        c.setUnscheduledReason(iv.getUnscheduledReason());
        return c;
    }

    private ReplanState state0(Disruption d) {
        ReplanState s = new ReplanState();
        s.index = schedulingService.rebuildIndexFromScheduled();
        s.liveState = new HashMap<>();
        for (Interview iv : interviewRepository.findAll())
            s.liveState.put(iv.getId(), cloneInterview(iv));
        return s;
    }

    private int day(Company c) {
        return c.getArrivalDay() != null ? c.getArrivalDay() : 1;
    }

    private boolean isCriticalMovement(ReplanLogEntry entry) {
        if (entry.oldTs == null || entry.newTs == null || entry.oldTs.equals(entry.newTs)) return false;
        Optional<TimeSlot> oldSlot = timeSlotRepository.findById(entry.oldTs);
        Optional<TimeSlot> newSlot = timeSlotRepository.findById(entry.newTs);
        if (oldSlot.isEmpty() || newSlot.isEmpty() || !oldSlot.get().getDay().equals(newSlot.get().getDay()))
            return false;
        return Math.abs(oldSlot.get().getSlotNumber() - newSlot.get().getSlotNumber()) * 45 <= 30;
    }

    private String describe(Disruption d) {
        String entityName = "entity#" + d.getTargetEntityId();
        try {
            switch (d.getType()) {
                case ROOM_UNAVAILABLE:
                    entityName = roomRepository.findById(d.getTargetEntityId()).get().getRoomNumber();
                    break;
                case PANEL_UNAVAILABLE:
                    entityName = panelRepository.findById(d.getTargetEntityId()).get().getName();
                    break;
                case COMPANY_LATE:
                    entityName = companyRepository.findById(d.getTargetEntityId()).get().getName();
                    break;
                case STUDENT_WITHDRAW:
                    entityName = studentRepository.findById(d.getTargetEntityId()).get().getName();
                    break;
            }
        } catch (Exception ignore) {}
        return "[" + entityName + " Day" + d.getDay() + " Slot" + d.getStartSlot() + "-" + d.getEndSlot() + "]";
    }
}
