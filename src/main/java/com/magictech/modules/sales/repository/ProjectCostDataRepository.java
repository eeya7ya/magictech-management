package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.ProjectCostData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCostDataRepository extends JpaRepository<ProjectCostData, Long> {

    Optional<ProjectCostData> findByProjectIdAndActiveTrue(Long projectId);

    Optional<ProjectCostData> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<ProjectCostData> findByActiveTrue();

    List<ProjectCostData> findByUploadedByIdAndActiveTrue(Long uploadedById);

    List<ProjectCostData> findByProjectReceivedConfirmationAndActiveTrue(Boolean confirmation);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
