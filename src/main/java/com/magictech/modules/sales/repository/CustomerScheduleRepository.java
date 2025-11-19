package com.magictech.modules/sales.repository;

import com.magictech.modules.sales.entity.CustomerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerScheduleRepository extends JpaRepository<CustomerSchedule, Long> {

    List<CustomerSchedule> findByCustomerIdAndActiveTrueOrderByStartDateAsc(Long customerId);

    List<CustomerSchedule> findByCustomerIdAndStatusAndActiveTrue(Long customerId, String status);

    List<CustomerSchedule> findByStartDateBetweenAndActiveTrue(LocalDate startDate, LocalDate endDate);

    long countByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerSchedule> findByActiveTrue();
}
