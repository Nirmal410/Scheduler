package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Interview;
import com.assigment.Scheduler.entity.ReplanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;

@Repository
public interface ReplanLogRepository extends JpaRepository<ReplanLog, Long> {
    List<ReplanLog> findByDisruptionId(Long disruptionId);
    List<ReplanLog> findByInterviewId(Long interviewId);
    List<ReplanLog> findByInterviewIn(Collection<Interview> interviews);
}
