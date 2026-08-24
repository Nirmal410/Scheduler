package com.assigment.Scheduler.repository;

import com.assigment.Scheduler.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByBranch(String branch);
    List<Student> findByCgpaGreaterThanEqual(Double cgpa);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.id = :id")
    void deleteByIdJpql(@Param("id") Long id);
}
