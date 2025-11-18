package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.SalesContract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Sales Contract Repository
 * Database access layer for SalesContract entity
 */
@Repository
public interface SalesContractRepository extends JpaRepository<SalesContract, Long> {

    /**
     * Find contract by sales order ID
     */
    Optional<SalesContract> findBySalesOrderIdAndActiveTrue(Long salesOrderId);

    /**
     * Find all active contracts
     */
    List<SalesContract> findByActiveTrue();

    /**
     * Find contracts by creator
     */
    List<SalesContract> findByCreatedByAndActiveTrue(String createdBy);
}