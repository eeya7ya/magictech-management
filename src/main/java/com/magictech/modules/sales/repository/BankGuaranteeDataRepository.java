package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.BankGuaranteeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BankGuaranteeDataRepository extends JpaRepository<BankGuaranteeData, Long> {

    Optional<BankGuaranteeData> findByProjectIdAndActiveTrue(Long projectId);

    Optional<BankGuaranteeData> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<BankGuaranteeData> findByActiveTrue();

    List<BankGuaranteeData> findByUploadedByIdAndActiveTrue(Long uploadedById);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
