package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.CoordinatorUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoordinatorUserRepository extends JpaRepository<CoordinatorUser, Long> {
    Optional<CoordinatorUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
