package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.CustomerElement;
import com.magictech.modules.sales.repository.CustomerElementRepository;
import com.magictech.modules.storage.entity.StorageItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerElementService {

    @Autowired
    private CustomerElementRepository repository;

    public List<CustomerElement> getCustomerElements(Long customerId) {
        return repository.findByCustomerIdAndActiveTrue(customerId);
    }

    public CustomerElement saveElement(CustomerElement element) {
        return repository.save(element);
    }

    public CustomerElement createElement(Customer customer, StorageItem item, int quantityNeeded, String createdBy) {
        CustomerElement element = new CustomerElement();
        element.setCustomer(customer);
        element.setStorageItem(item);
        element.setQuantityNeeded(quantityNeeded);
        element.setUnitPrice(item.getPrice());
        element.setCreatedBy(createdBy);
        element.setStatus("PENDING");
        return repository.save(element);
    }

    public Optional<CustomerElement> getElementById(Long id) {
        return repository.findById(id);
    }

    public CustomerElement updateElement(CustomerElement updatedElement) {
        return repository.findById(updatedElement.getId())
                .map(existing -> {
                    existing.setQuantityNeeded(updatedElement.getQuantityNeeded());
                    existing.setQuantityAllocated(updatedElement.getQuantityAllocated());
                    existing.setStatus(updatedElement.getStatus());
                    existing.setUnitPrice(updatedElement.getUnitPrice());
                    existing.setNotes(updatedElement.getNotes());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Element not found: " + updatedElement.getId()));
    }

    public void deleteElement(Long id) {
        repository.findById(id).ifPresent(element -> {
            element.setActive(false);
            repository.save(element);
        });
    }

    public long getElementCount(Long customerId) {
        return repository.countByCustomerIdAndActiveTrue(customerId);
    }

    public java.math.BigDecimal calculateTotalCost(Long customerId) {
        List<CustomerElement> elements = repository.findByCustomerIdAndActiveTrue(customerId);
        return elements.stream()
                .map(CustomerElement::getTotalPrice)
                .filter(price -> price != null)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
