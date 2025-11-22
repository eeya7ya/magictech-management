package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.SiteSurveyData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SiteSurveyDataRepository extends JpaRepository<SiteSurveyData, Long> {

    Optional<SiteSurveyData> findByProjectIdAndActiveTrue(Long projectId);

    Optional<SiteSurveyData> findByWorkflowIdAndActiveTrue(Long workflowId);

    List<SiteSurveyData> findByActiveTrue();

    List<SiteSurveyData> findBySurveyDoneByAndActiveTrue(String surveyDoneBy);

    boolean existsByProjectIdAndActiveTrue(Long projectId);
}
