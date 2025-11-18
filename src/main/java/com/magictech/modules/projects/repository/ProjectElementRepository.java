package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.ProjectElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectElementRepository extends JpaRepository<ProjectElement, Long> {

    List<ProjectElement> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectElement> findByStorageItemIdAndActiveTrue(Long storageItemId);

    @Query("SELECT pe FROM ProjectElement pe WHERE pe.project.id = :projectId AND pe.active = true")
    List<ProjectElement> findElementsByProjectId(Long projectId);

    long countByProjectIdAndActiveTrue(Long projectId);
}