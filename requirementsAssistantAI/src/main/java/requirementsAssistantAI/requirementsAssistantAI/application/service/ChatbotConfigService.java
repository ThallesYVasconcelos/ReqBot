package requirementsAssistantAI.requirementsAssistantAI.application.service;

import requirementsAssistantAI.requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.RequirementSetRepository;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import requirementsAssistantAI.requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.requirementsAssistantAI.dto.CreateChatbotConfigRequest;

@Service
public class ChatbotConfigService {

    private final ChatbotConfigRepository chatbotConfigRepository;
    private final RequirementSetRepository requirementSetRepository;

    public ChatbotConfigService(
            ChatbotConfigRepository chatbotConfigRepository,
            RequirementSetRepository requirementSetRepository) {
        this.chatbotConfigRepository = chatbotConfigRepository;
        this.requirementSetRepository = requirementSetRepository;
    }

    @Transactional
    public ChatbotConfigDTO createOrUpdateConfig(@Valid CreateChatbotConfigRequest request) {
        chatbotConfigRepository.findByIsActiveTrue().ifPresent(config -> {
            config.setIsActive(false);
            chatbotConfigRepository.save(config);
        });

        RequirementSet requirementSet = requirementSetRepository.findById(
                Objects.requireNonNull(request.getRequirementSetId()))
                .orElseThrow(() -> new RuntimeException("RequirementSet não encontrado com ID: " + request.getRequirementSetId()));

        ChatbotConfig config = new ChatbotConfig(
                requirementSet,
                request.getStartTime(),
                request.getEndTime()
        );
        config.setIsActive(true);
        config = chatbotConfigRepository.save(config);
        return convertToDTO(config);
    }

    public ChatbotConfigDTO getActiveConfig() {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("Nenhuma configuração de chatbot ativa encontrada"));
        return convertToDTO(config);
    }

    public List<ChatbotConfigDTO> getAllConfigs() {
        return chatbotConfigRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ChatbotConfigDTO getConfigById(@NonNull UUID id) {
        ChatbotConfig config = chatbotConfigRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));
        return convertToDTO(config);
    }

    @Transactional
    public ChatbotConfigDTO toggleConfig(@NonNull UUID id, Boolean isActive) {
        ChatbotConfig config = chatbotConfigRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        if (Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(config.getIsActive())) {
            chatbotConfigRepository.findByIsActiveTrue().ifPresent(otherConfig -> {
                otherConfig.setIsActive(false);
                chatbotConfigRepository.save(otherConfig);
            });
        }

        config.setIsActive(isActive);
        config = chatbotConfigRepository.save(config);
        return convertToDTO(config);
    }

    @Transactional
    public void deleteConfig(@NonNull UUID id) {
        ChatbotConfig config = chatbotConfigRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));
        chatbotConfigRepository.delete(config);
    }

    private ChatbotConfigDTO convertToDTO(ChatbotConfig config) {
        RequirementSet requirementSet = config.getRequirementSet();
        return new ChatbotConfigDTO(
                config.getId(),
                config.getIsActive(),
                requirementSet != null ? requirementSet.getId() : null,
                requirementSet != null ? requirementSet.getName() : null,
                config.getStartTime(),
                config.getEndTime(),
                config.getCreatedAt(),
                config.getUpdatedAt(),
                config.isAvailableNow()
        );
    }
}
