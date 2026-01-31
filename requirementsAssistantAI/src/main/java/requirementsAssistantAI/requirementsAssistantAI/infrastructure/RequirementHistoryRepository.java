package com.tcc.requirements_assistant_api.repository;

import com.tcc.requirements_assistant_api.model.RequirementHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RequirementHistoryRepository extends JpaRepository<RequirementHistory, UUID> {
    List<RequirementHistory> findByRequirement_UuidOrderByCreatedAtDesc(UUID requirementUuid);
}

