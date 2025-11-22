package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.MissingItemRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissingItemRequestRepository extends JpaRepository<MissingItemRequest, Long> {

    List<MissingItemRequest> findByProjectIdAndActiveTrue(Long projectId);

    List<MissingItemRequest> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<MissingItemRequest> findByActiveTrue();

    List<MissingItemRequest> findByApprovalStatusAndActiveTrue(MissingItemRequest.ApprovalStatus status);

    List<MissingItemRequest> findByRequestedByIdAndActiveTrue(Long requestedById);

    List<MissingItemRequest> findByItemDeliveredAndActiveTrue(Boolean delivered);

    Optional<MissingItemRequest> findTopByProjectIdAndActiveTrueOrderByRequestedAtDesc(Long projectId);

    boolean existsByProjectIdAndApprovalStatusAndActiveTrue(Long projectId, MissingItemRequest.ApprovalStatus status);
}
