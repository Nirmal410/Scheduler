package com.assigment.Scheduler.service;

import com.assigment.Scheduler.dto.MetricsDTO;
import com.assigment.Scheduler.entity.*;
import com.assigment.Scheduler.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetricsService {

    private final StudentRepository studentRepository;
    private final CompanyRepository companyRepository;
    private final RoomRepository roomRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ShortlistRepository shortlistRepository;
    private final InterviewRepository interviewRepository;
    private final ReplanLogRepository replanLogRepository;
    private final DisruptionRepository disruptionRepository;

    public MetricsService(StudentRepository studentRepository,
            CompanyRepository companyRepository, RoomRepository roomRepository,
            TimeSlotRepository timeSlotRepository, ShortlistRepository shortlistRepository,
            InterviewRepository interviewRepository, ReplanLogRepository replanLogRepository,
            DisruptionRepository disruptionRepository) {
        this.studentRepository = studentRepository;
        this.companyRepository = companyRepository;
        this.roomRepository = roomRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.shortlistRepository = shortlistRepository;
        this.interviewRepository = interviewRepository;
        this.replanLogRepository = replanLogRepository;
        this.disruptionRepository = disruptionRepository;
    }

    public MetricsDTO computeAllMetrics() {
        MetricsDTO m = new MetricsDTO();
        m.setTotalStudents(studentRepository.count());
        m.setTotalCompanies(companyRepository.count());
        m.setTotalRooms(roomRepository.count());
        m.setTotalTimeSlots(timeSlotRepository.count());
        m.setTotalShortlists(shortlistRepository.count());

        long scheduled = interviewRepository.countByStatus(InterviewStatus.SCHEDULED);
        long moved = interviewRepository.countByStatus(InterviewStatus.MOVED);
        long unscheduled = interviewRepository.countByStatus(InterviewStatus.UNSCHEDULED)
            + interviewRepository.countByStatus(InterviewStatus.REPLAN_REQUIRED)
            + interviewRepository.countByStatus(InterviewStatus.COORDINATOR_REVIEW);
        long cancelled = interviewRepository.countByStatus(InterviewStatus.CANCELLED);

        long totalScheduled = scheduled + moved;
        m.setInterviewsScheduled(totalScheduled);
        m.setInterviewsUnscheduled(unscheduled + cancelled);
        m.setInterviewsMoved(moved);
        long totalInterviews = interviewRepository.count();
        m.setSchedulingRatePercent(
                totalInterviews == 0 ? 0.0 : Math.round(totalScheduled * 10000.0 / totalInterviews) / 100.0);

        m.setOverallRoomUtilizationPercent(computeRoomUtilization());

        long[] wait = computeStudentWait();
        m.setAverageStudentWaitMinutes(wait[0]);
        m.setMaxStudentWaitMinutes(wait[1]);

        m.setStudentConflictCount(computeStudentConflicts());

        m.setTotalDisruptionsProcessed(disruptionRepository.count());
        m.setAverageReplanChurnRatio(computeAverageChurn());

        return m;
    }

    private double computeRoomUtilization() {
        long totalPossible = roomRepository.count() * timeSlotRepository.count();
        long bookedSlots = 0;
        for (Interview iv : interviewRepository.findAllActiveScheduled()) {
            if (iv.getRoom() != null && iv.getTimeSlot() != null)
                bookedSlots++;
        }
        return totalPossible == 0 ? 0.0 : Math.round(bookedSlots * 10000.0 / totalPossible) / 100.0;
    }

    private long[] computeStudentWait() {
        int SLOT_MINUTES = 45;
        Map<Long, Map<Integer, List<Integer>>> studentDaySlots = new HashMap<>();
        for (Interview iv : interviewRepository.findAllActiveScheduled()) {
            if (iv.getTimeSlot() == null)
                continue;
            studentDaySlots.computeIfAbsent(iv.getStudent().getId(), k -> new HashMap<>())
                    .computeIfAbsent(iv.getTimeSlot().getDay(), k -> new ArrayList<>())
                    .add(iv.getTimeSlot().getSlotNumber());
        }
        long total = 0;
        long count = 0;
        long max = 0;
        for (Map.Entry<Long, Map<Integer, List<Integer>>> e1 : studentDaySlots.entrySet()) {
            for (Map.Entry<Integer, List<Integer>> e2 : e1.getValue().entrySet()) {
                List<Integer> slots = e2.getValue();
                if (slots.size() < 2)
                    continue;
                Collections.sort(slots);
                for (int i = 1; i < slots.size(); i++) {
                    long gap = (slots.get(i) - slots.get(i - 1) - 1) * (long) SLOT_MINUTES;
                    if (gap < 0)
                        gap = 0;
                    total += gap;
                    count++;
                    if (gap > max)
                        max = gap;
                }
            }
        }
        long avg = count == 0 ? 0 : total / count;
        return new long[] { avg, max };
    }

    private long computeStudentConflicts() {
        Map<Long, Set<Long>> studentTs = new HashMap<>();
        long conflicts = 0;
        for (Interview iv : interviewRepository.findAllActiveScheduled()) {
            if (iv.getTimeSlot() == null)
                continue;
            Set<Long> seen = studentTs.computeIfAbsent(iv.getStudent().getId(), k -> new HashSet<>());
            if (seen.contains(iv.getTimeSlot().getId()))
                conflicts++;
            else
                seen.add(iv.getTimeSlot().getId());
        }
        return conflicts;
    }

    private double computeAverageChurn() {
        List<Disruption> disruptions = disruptionRepository.findAll();
        if (disruptions.isEmpty())
            return 0.0;
        double totalChurn = 0.0;
        int counted = 0;
        for (Disruption d : disruptions) {
            List<ReplanLog> logs = replanLogRepository.findByDisruptionId(d.getId());
            if (logs.isEmpty())
                continue;
            long directlyAffected = logs.stream().mapToLong(l -> l.getCascadeDepth() == 0 ? 1 : 0).sum();
            long totalMoves = logs.size();
            if (directlyAffected > 0) {
                totalChurn += (double) totalMoves / directlyAffected;
                counted++;
            }
        }
        return counted == 0 ? 0.0 : Math.round(totalChurn * 100.0 / counted) / 100.0;
    }
}
