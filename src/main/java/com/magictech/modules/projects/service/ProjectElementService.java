package com.magictech.modules.projects.service;

import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.repository.ProjectElementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectElementService {

    @Autowired
    private ProjectElementRepository elementRepository;

    @Transactional
    public ProjectElement createElement(ProjectElement element) {
        return elementRepository.save(element);
    }

    @Transactional(readOnly = true)
    public List<ProjectElement> getElementsByProject(Long projectId) {
        return elementRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * ✅ NEW METHOD - Add this to your service
     */
    @Transactional(readOnly = true)
    public ProjectElement getElementById(Long id) {
        ProjectElement element = elementRepository.findById(id).orElse(null);

        if (element != null && element.getStorageItem() != null) {
            // Force initialization of StorageItem within transaction
            element.getStorageItem().getProductName();
            element.getStorageItem().getQuantity();
            element.getStorageItem().getId();
        }

        return element;
    }

    @Transactional
    public ProjectElement updateElement(Long id, ProjectElement updatedElement) {
        ProjectElement existing = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found"));

        existing.setQuantityNeeded(updatedElement.getQuantityNeeded());
        existing.setQuantityAllocated(updatedElement.getQuantityAllocated());
        existing.setNotes(updatedElement.getNotes());
        existing.setStatus(updatedElement.getStatus());

        return elementRepository.save(existing);
    }

    @Transactional
    public void deleteElement(Long id) {
        ProjectElement element = elementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Element not found"));
        element.setActive(false);
        elementRepository.save(element);
    }
}










