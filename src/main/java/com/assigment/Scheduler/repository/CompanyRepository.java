package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Company;
import com.assigment.Scheduler.entity.CompanyTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByTier(CompanyTier tier);
    Optional<Company> findByName(String name);
}
