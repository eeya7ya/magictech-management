package com.magictech.modules.projects.service;

import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.ProjectSchedule;
import com.magictech.modules.projects.repository.ProjectScheduleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectScheduleService {

    @Autowired
    private ProjectScheduleRepository repository;

    public List<ProjectSchedule> getSchedulesByProject(Long projectId) {
        return repository.findByProjectIdOrderByStartDateAsc(projectId);
    }

    public ProjectSchedule createSchedule(ProjectSchedule schedule) {
        return repository.save(schedule);
    }

    public ProjectSchedule updateSchedule(Long id, ProjectSchedule updated) {
        ProjectSchedule existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        existing.setTaskName(updated.getTaskName());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());
        existing.setProgress(updated.getProgress());
        existing.setDescription(updated.getDescription());
        existing.setAssignedTo(updated.getAssignedTo());

        return repository.save(existing);
    }

    public void deleteSchedule(Long id) {
        repository.deleteById(id);
    }

    public long getScheduleCount(Long projectId) {
        return repository.countByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Alias method for getSchedulesByProject - for compatibility
     */
    public List<ProjectSchedule> getProjectSchedules(Long projectId) {
        return getSchedulesByProject(projectId);
    }
}