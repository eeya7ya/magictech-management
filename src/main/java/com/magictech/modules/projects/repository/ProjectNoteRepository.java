package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.ProjectNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectNoteRepository extends JpaRepository<ProjectNote, Long> {

    List<ProjectNote> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectNote> findByProjectIdOrderByLastUpdatedDesc(Long projectId);
}