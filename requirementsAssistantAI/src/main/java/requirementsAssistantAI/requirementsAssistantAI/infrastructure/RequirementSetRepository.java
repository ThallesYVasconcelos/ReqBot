package com.tcc.requirements_assistant_api.repository;

import com.tcc.requirements_assistant_api.model.RequirementSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RequirementSetRepository extends JpaRepository<RequirementSet, UUID> {
}

