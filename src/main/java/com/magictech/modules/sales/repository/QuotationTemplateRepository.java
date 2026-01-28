package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.QuotationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationTemplateRepository extends JpaRepository<QuotationTemplate, Long> {

    List<QuotationTemplate> findByActiveTrue();

    Optional<QuotationTemplate> findByIdAndActiveTrue(Long id);

    Optional<QuotationTemplate> findByIsDefaultTrueAndActiveTrue();

    List<QuotationTemplate> findByTemplateNameContainingIgnoreCaseAndActiveTrue(String name);

    @Query("SELECT t FROM QuotationTemplate t WHERE t.active = true ORDER BY t.isDefault DESC, t.templateName ASC")
    List<QuotationTemplate> findAllOrderedByDefault();
}
