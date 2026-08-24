package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.ReplanLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReplanLogRepository extends JpaRepository<ReplanLog, Long> {
    List<ReplanLog> findByDisruptionId(Long disruptionId);
}
