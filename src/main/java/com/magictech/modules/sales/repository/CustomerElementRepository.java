package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.CustomerElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerElementRepository extends JpaRepository<CustomerElement, Long> {

    List<CustomerElement> findByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerElement> findByCustomerIdAndStatusAndActiveTrue(Long customerId, String status);

    long countByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerElement> findByActiveTrue();
}
