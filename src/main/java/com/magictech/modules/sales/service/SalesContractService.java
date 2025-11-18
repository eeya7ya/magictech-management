package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.SalesContract;
import com.magictech.modules.sales.repository.SalesContractRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Sales Contract Service - FIXED
 * Business logic for contract/requirements management
 */
@Service
@Transactional
public class SalesContractService {

    @Autowired
    private SalesContractRepository salesContractRepository;

    /**
     * Get contract by sales order ID
     */
    public Optional<SalesContract> getContractBySalesOrderId(Long salesOrderId) {
        return salesContractRepository.findBySalesOrderIdAndActiveTrue(salesOrderId);
    }

    /**
     * Get all active contracts
     */
    public List<SalesContract> getAllContracts() {
        return salesContractRepository.findByActiveTrue();
    }

    /**
     * Get contract by ID
     */
    public Optional<SalesContract> getContractById(Long id) {
        return salesContractRepository.findById(id);
    }

    /**
     * Create new contract
     */
    public SalesContract createContract(SalesContract contract) {
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        contract.setActive(true);
        return salesContractRepository.save(contract);
    }

    /**
     * Update existing contract
     */
    public SalesContract updateContract(Long id, SalesContract contractDetails) {
        SalesContract existing = salesContractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));

        // Update fields
        existing.setTitle(contractDetails.getTitle());
        existing.setRequirements(contractDetails.getRequirements());
        existing.setTermsAndConditions(contractDetails.getTermsAndConditions());
        existing.setDeliveryTerms(contractDetails.getDeliveryTerms());
        existing.setPaymentTerms(contractDetails.getPaymentTerms());
        existing.setWarrantyInfo(contractDetails.getWarrantyInfo());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(contractDetails.getUpdatedBy());

        return salesContractRepository.save(existing);
    }

    /**
     * Delete contract (soft delete)
     */
    public void deleteContract(Long id) {
        SalesContract contract = salesContractRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contract not found with id: " + id));
        contract.setActive(false);
        contract.setUpdatedAt(LocalDateTime.now());
        salesContractRepository.save(contract);
    }

    /**
     * Get contracts by creator
     */
    public List<SalesContract> getContractsByCreator(String createdBy) {
        return salesContractRepository.findByCreatedByAndActiveTrue(createdBy);
    }
}