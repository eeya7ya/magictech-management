package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.SizingPricingData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SizingPricingDataRepository extends JpaRepository<SizingPricingData, Long> {

    Optional<SizingPricingData> findByProjectIdAndActiveTrue(Long projectId);

    Optional<SizingPricingData> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<SizingPricingData> findByActiveTrue();

    List<SizingPricingData> findByUploadedByIdAndActiveTrue(Long uploadedById);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
