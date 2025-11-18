package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Customer Repository
 * Database access layer for Customer entity
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by ID (active only)
     */
    Optional<Customer> findByIdAndActiveTrue(Long id);

    /**
     * Find all active customers
     */
    List<Customer> findByActiveTrue();

    /**
     * Find customers by name (case-insensitive, partial match)
     */
    List<Customer> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    /**
     * Find customer by email
     */
    Optional<Customer> findByEmailAndActiveTrue(String email);

    /**
     * Find customer by phone
     */
    Optional<Customer> findByPhoneAndActiveTrue(String phone);

    /**
     * Check if customer exists
     */
    boolean existsByIdAndActiveTrue(Long id);

    /**
     * Count total active customers
     */
    long countByActiveTrue();
}