package com.tcc.requirements_assistant_api.repository;

import com.tcc.requirements_assistant_api.model.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RequirementRepository extends JpaRepository<Requirement, UUID> {
    Optional<Requirement> findByRequirementId(String requirementId);
    List<Requirement> findByRequirementSetId(UUID requirementSetId);
}

