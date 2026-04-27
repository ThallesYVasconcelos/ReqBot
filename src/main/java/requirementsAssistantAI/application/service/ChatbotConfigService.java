package requirementsAssistantAI.application.service;

import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;
import requirementsAssistantAI.infrastructure.WorkspaceRepository;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.dto.CreateChatbotConfigRequest;

@Service
public class ChatbotConfigService {

    private final ChatbotConfigRepository chatbotConfigRepository;
    private final RequirementSetRepository requirementSetRepository;
    private final WorkspaceRepository workspaceRepository;

    public ChatbotConfigService(
            ChatbotConfigRepository chatbotConfigRepository,
            RequirementSetRepository requirementSetRepository,
            WorkspaceRepository workspaceRepository) {
        this.chatbotConfigRepository = chatbotConfigRepository;
        this.requirementSetRepository = requirementSetRepository;
        this.workspaceRepository = workspaceRepository;
    }

    @Transactional
    public ChatbotConfigDTO createOrUpdateConfig(@Valid CreateChatbotConfigRequest request) {
        RequirementSet requirementSet = requirementSetRepository.findById(
                Objects.requireNonNull(request.getRequirementSetId()))
                .orElseThrow(() -> new ResourceNotFoundException("RequirementSet (projeto)", request.getRequirementSetId()));

        Workspace workspace = requirementSet.getWorkspace();

        if (workspace != null) {
            chatbotConfigRepository.findByIsActiveTrueAndWorkspace_Id(workspace.getId())
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        chatbotConfigRepository.save(existing);
                    });
        } else {
            chatbotConfigRepository.findByIsActiveTrue().ifPresent(existing -> {
                existing.setIsActive(false);
                chatbotConfigRepository.save(existing);
            });
        }

        ChatbotConfig config = new ChatbotConfig(
                requirementSet,
                request.getStartTime(),
                request.getEndTime()
        );
        config.setIsActive(true);
        config.setShowRequirementsToUsers(Boolean.TRUE.equals(request.getShowRequirementsToUsers()));
        config = chatbotConfigRepository.save(config);
        return convertToDTO(config);
    }

    @Transactional(readOnly = true)
    public ChatbotConfigDTO getActiveConfigByWorkspace(UUID workspaceId) {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueAndWorkspace_Id(workspaceId)
                .orElseThrow(() -> new RuntimeException("Nenhuma configuração de chatbot ativa para este workspace"));
        return convertToDTO(config);
    }

    @Transactional(readOnly = true)
    public List<ChatbotConfigDTO> getConfigsByWorkspace(UUID workspaceId) {
        return chatbotConfigRepository.findAllByWorkspace_Id(workspaceId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatbotConfigDTO getActiveConfig() {
        ChatbotConfig config = chatbotConfigRepository.findByIsActiveTrueWithRequirementSet()
                .orElseThrow(() -> new RuntimeException("Nenhuma configuração de chatbot ativa encontrada"));
        return convertToDTO(config);
    }

    @Transactional(readOnly = true)
    public List<ChatbotConfigDTO> getAllConfigs() {
        return chatbotConfigRepository.findAllWithRequirementSet().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatbotConfigDTO getConfigById(@NonNull UUID id) {
        ChatbotConfig config = chatbotConfigRepository.findByIdWithRequirementSet(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));
        return convertToDTO(config);
    }

    @Transactional
    public ChatbotConfigDTO toggleConfig(@NonNull UUID id, Boolean isActive) {
        ChatbotConfig config = chatbotConfigRepository.findByIdWithRequirementSet(Objects.requireNonNull(id))
                .orElseThrow(() -> new RuntimeException("Configuração não encontrada com ID: " + id));

        if (Boolean.TRUE.equals(isActive) && !Boolean.TRUE.equals(config.getIsActive())) {
            chatbotConfigRepository.findByIsActiveTrue().ifPresent(otherConfig -> {
                otherConfig.setIsActive(false);
                chatbotConfigRepository.save(otherConfig);
            });
        }

        config.setIsActive(isActive);
        config = chatbotConfigRepository.save(config);
        // Força o carregamento do RequirementSet antes de converter para DTO
        if (config.getRequirementSet() != null) {
            config.getRequirementSet().getId();
        }
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
        Workspace workspace = config.getWorkspace();
        return new ChatbotConfigDTO(
                config.getId(),
                config.getIsActive(),
                requirementSet != null ? requirementSet.getId() : null,
                requirementSet != null ? requirementSet.getName() : null,
                workspace != null ? workspace.getId() : null,
                workspace != null ? workspace.getName() : null,
                config.getStartTime(),
                config.getEndTime(),
                config.getShowRequirementsToUsers(),
                config.getCreatedAt(),
                config.getUpdatedAt(),
                config.isAvailableNow()
        );
    }
}
