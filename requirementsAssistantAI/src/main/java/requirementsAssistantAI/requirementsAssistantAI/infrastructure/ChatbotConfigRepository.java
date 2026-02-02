package com.tcc.requirements_assistant_api.repository;

import com.tcc.requirements_assistant_api.model.ChatbotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatbotConfigRepository extends JpaRepository<ChatbotConfig, UUID> {
    Optional<ChatbotConfig> findByIsActiveTrue();
}
