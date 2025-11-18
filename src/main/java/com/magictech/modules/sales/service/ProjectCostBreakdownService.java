package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.ProjectCostBreakdown;
import com.magictech.modules.sales.repository.ProjectCostBreakdownRepository;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.service.ProjectElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing project cost breakdowns
 */
@Service
@Transactional
public class ProjectCostBreakdownService {

    @Autowired
    private ProjectCostBreakdownRepository breakdownRepository;

    @Autowired
    private ProjectElementService elementService;

    /**
     * Get or create cost breakdown for project
     */
    public ProjectCostBreakdown getOrCreateBreakdown(Long projectId, String createdBy) {
        Optional<ProjectCostBreakdown> existing = breakdownRepository.findByProjectId(projectId);
        if (existing.isPresent()) {
            return existing.get();
        }

        ProjectCostBreakdown breakdown = new ProjectCostBreakdown();
        breakdown.setProjectId(projectId);
        breakdown.setCreatedBy(createdBy);
        updateElementsSubtotal(breakdown);
        return breakdownRepository.save(breakdown);
    }

    /**
     * Update elements subtotal from project elements
     */
    public void updateElementsSubtotal(ProjectCostBreakdown breakdown) {
        List<ProjectElement> elements = elementService.getElementsByProject(breakdown.getProjectId());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (ProjectElement element : elements) {
            if (element.getStorageItem() != null && element.getStorageItem().getPrice() != null) {
                BigDecimal elementPrice = element.getStorageItem().getPrice();
                BigDecimal quantity = new BigDecimal(element.getQuantityNeeded());
                subtotal = subtotal.add(elementPrice.multiply(quantity));
            }
        }

        breakdown.setElementsSubtotal(subtotal);
        breakdown.calculateTotals();
    }

    /**
     * Save or update cost breakdown
     */
    public ProjectCostBreakdown saveBreakdown(ProjectCostBreakdown breakdown, String updatedBy) {
        breakdown.setUpdatedBy(updatedBy);
        updateElementsSubtotal(breakdown);
        return breakdownRepository.save(breakdown);
    }

    /**
     * Get cost breakdown by project ID
     */
    public Optional<ProjectCostBreakdown> getBreakdownByProject(Long projectId) {
        return breakdownRepository.findByProjectId(projectId);
    }

    /**
     * Delete cost breakdown
     */
    public void deleteBreakdown(Long projectId) {
        breakdownRepository.deleteByProjectId(projectId);
    }

    /**
     * Refresh cost breakdown (recalculate from current elements)
     */
    public ProjectCostBreakdown refreshBreakdown(Long projectId, String updatedBy) {
        Optional<ProjectCostBreakdown> existing = breakdownRepository.findByProjectId(projectId);
        if (existing.isEmpty()) {
            return getOrCreateBreakdown(projectId, updatedBy);
        }

        ProjectCostBreakdown breakdown = existing.get();
        breakdown.setUpdatedBy(updatedBy);
        updateElementsSubtotal(breakdown);
        return breakdownRepository.save(breakdown);
    }
}
