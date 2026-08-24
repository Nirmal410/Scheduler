package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Disruption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisruptionRepository extends JpaRepository<Disruption, Long> {
}
