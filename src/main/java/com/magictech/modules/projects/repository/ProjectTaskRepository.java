package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {

    List<ProjectTask> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectTask> findByProjectIdAndIsCompletedAndActiveTrue(Long projectId, Boolean isCompleted);

    long countByProjectIdAndIsCompletedAndActiveTrue(Long projectId, Boolean isCompleted);

    long countByProjectIdAndActiveTrue(Long projectId);
}