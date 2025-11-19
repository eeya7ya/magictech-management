package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findByCustomerIdAndActiveTrueOrderByDateAddedDesc(Long customerId);

    long countByCustomerIdAndActiveTrue(Long customerId);

    List<CustomerNote> findByActiveTrue();
}
