package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.ProjectSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectScheduleRepository extends JpaRepository<ProjectSchedule, Long> {

    List<ProjectSchedule> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectSchedule> findByProjectIdOrderByStartDateAsc(Long projectId);

    List<ProjectSchedule> findByStatusAndActiveTrue(String status);

    long countByProjectIdAndActiveTrue(Long projectId);
}