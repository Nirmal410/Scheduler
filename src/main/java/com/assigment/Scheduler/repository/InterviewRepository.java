package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Interview;
import com.assigment.Scheduler.entity.InterviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Collection;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByStatus(InterviewStatus status);
    List<Interview> findByStatusIn(Collection<InterviewStatus> statuses);
    List<Interview> findByStudentId(Long studentId);
    List<Interview> findByCompanyId(Long companyId);
    List<Interview> findByTimeSlotDay(Integer day);
    List<Interview> findByPanelIdAndTimeSlotIsNotNull(Long panelId);
    List<Interview> findByRoomIdAndTimeSlotIsNotNull(Long roomId);
    List<Interview> findByRoomId(Long roomId);
    List<Interview> findByPanelId(Long panelId);

    @Query("SELECT i FROM Interview i WHERE i.status = com.assigment.Scheduler.entity.InterviewStatus.SCHEDULED OR i.status = com.assigment.Scheduler.entity.InterviewStatus.MOVED")
    List<Interview> findAllActiveScheduled();

    long countByStatus(InterviewStatus status);
}
