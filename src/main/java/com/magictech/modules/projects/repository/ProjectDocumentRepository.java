package com.magictech.modules.projects.repository;

import com.magictech.modules.projects.entity.ProjectDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectDocumentRepository extends JpaRepository<ProjectDocument, Long> {

    List<ProjectDocument> findByProjectIdAndActiveTrue(Long projectId);

    List<ProjectDocument> findByProjectIdAndCategoryAndActiveTrue(Long projectId, String category);

    List<ProjectDocument> findByActiveTrue();

    long countByProjectIdAndActiveTrue(Long projectId);
}
