package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findByDay(Integer day);
    List<TimeSlot> findByDayAndSlotNumberBetween(Integer day, Integer startSlot, Integer endSlot);
    Optional<TimeSlot> findByDayAndSlotNumber(Integer day, Integer slotNumber);
}
