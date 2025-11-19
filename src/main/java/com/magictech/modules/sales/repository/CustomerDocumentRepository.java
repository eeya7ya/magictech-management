package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.CustomerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerDocumentRepository extends JpaRepository<CustomerDocument, Long> {

    List<CustomerDocument> findByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerDocument> findByCustomerIdAndCategoryAndActiveTrue(Long customerId, String category);

    List<CustomerDocument> findByActiveTrue();

    long countByCustomerIdAndActiveTrue(Long customerId);
}
