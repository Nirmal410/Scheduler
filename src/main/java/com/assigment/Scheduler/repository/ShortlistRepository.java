package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Shortlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ShortlistRepository extends JpaRepository<Shortlist, Long> {
    List<Shortlist> findByCompanyId(Long companyId);
    List<Shortlist> findByStudentId(Long studentId);
}
