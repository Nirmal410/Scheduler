package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Panel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PanelRepository extends JpaRepository<Panel, Long> {
    List<Panel> findByCompanyId(Long companyId);
}
