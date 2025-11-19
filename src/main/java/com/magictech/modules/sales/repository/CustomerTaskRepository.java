package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.CustomerTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTaskRepository extends JpaRepository<CustomerTask, Long> {

    List<CustomerTask> findByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerTask> findByCustomerIdAndIsCompletedAndActiveTrue(Long customerId, Boolean isCompleted);

    long countByCustomerIdAndIsCompletedAndActiveTrue(Long customerId, Boolean isCompleted);

    List<CustomerTask> findByActiveTrue();
}
