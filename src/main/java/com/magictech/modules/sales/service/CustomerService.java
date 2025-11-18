package com.magictech.modules.sales.service;

import com.magictech.core.notification.NotificationService;
import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Customer Service - FIXED
 * Business logic for customer management
 */
@Service
@Transactional
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Get all active customers
     */
    public List<Customer> getAllCustomers() {
        return customerRepository.findByActiveTrue();
    }

    /**
     * Get customer by ID
     */
    public Optional<Customer> getCustomerById(Long id) {
        return customerRepository.findByIdAndActiveTrue(id);
    }

    /**
     * Create new customer
     */
    public Customer createCustomer(Customer customer) {
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    /**
     * Update existing customer
     */
    public Customer updateCustomer(Long id, Customer customerDetails) {
        Customer existing = customerRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));

        String oldStatus = existing.getStatus();

        // Update fields
        existing.setName(customerDetails.getName());
        existing.setEmail(customerDetails.getEmail());
        existing.setPhone(customerDetails.getPhone());
        existing.setAddress(customerDetails.getAddress());
        existing.setCompany(customerDetails.getCompany());
        existing.setNotes(customerDetails.getNotes());
        existing.setStatus(customerDetails.getStatus());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(customerDetails.getUpdatedBy());

        Customer saved = customerRepository.save(existing);

        // If customer status changed to "Completed", notify PRICING module
        if (customerDetails.getStatus() != null &&
            customerDetails.getStatus().equalsIgnoreCase("Completed") &&
            !customerDetails.getStatus().equalsIgnoreCase(oldStatus)) {

            notificationService.createNotificationWithRelation(
                "PRICING",  // targetRole
                "PRICING",  // module
                "CUSTOMER_COMPLETED",  // type
                "Customer Order Completed",  // title
                String.format("Customer '%s' order has been completed and needs pricing finalization", saved.getName()),  // message
                saved.getId(),  // relatedId
                "CUSTOMER",  // relatedType
                "HIGH",  // priority
                existing.getUpdatedBy() != null ? existing.getUpdatedBy() : "System"  // createdBy
            );

            System.out.println("✓ Pricing notification created for completed customer: " + saved.getName());
        }

        return saved;
    }

    /**
     * Delete customer (soft delete)
     */
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.setActive(false);
        customer.setUpdatedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    /**
     * Search customers by name
     */
    public List<Customer> searchCustomersByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return getAllCustomers();
        }
        return customerRepository.findByNameContainingIgnoreCaseAndActiveTrue(name.trim());
    }

    /**
     * Get total customer count
     */
    public long getTotalCustomerCount() {
        return customerRepository.countByActiveTrue();
    }

    /**
     * Check if customer exists
     */
    public boolean customerExists(Long id) {
        return customerRepository.existsByIdAndActiveTrue(id);
    }
}