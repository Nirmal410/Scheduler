package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.ResourceAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceAvailabilityRepository extends JpaRepository<ResourceAvailability, Long> {
    List<ResourceAvailability> findByResourceTypeAndResourceId(String resourceType, Long resourceId);
    void deleteByResourceTypeAndResourceId(String resourceType, Long resourceId);
}
