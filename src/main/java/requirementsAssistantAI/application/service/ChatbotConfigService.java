package requirementsAssistantAI.application.service;

import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.application.exception.ResourceNotFoundException;
import requirementsAssistantAI.domain.ChatbotConfig;
import requirementsAssistantAI.domain.RequirementSet;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.dto.CreateChatbotConfigRequest;
import requirementsAssistantAI.infrastructure.ChatbotConfigRepository;
import requirementsAssistantAI.infrastructure.RequirementSetRepository;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChatbotConfigService {

    private static final int CODE_GENERATION_ATTEMPTS = 5;

    private final ChatbotConfigRepository chatbotRepository;
    private final RequirementSetRepository projectRepository;
    private final WorkspaceAuthorizationService authorizationService;
    private final ChatbotAccessService accessService;
    private final ChatbotCodeService codeService;

    public ChatbotConfigService(
            ChatbotConfigRepository chatbotRepository,
            RequirementSetRepository projectRepository,
            WorkspaceAuthorizationService authorizationService,
            ChatbotAccessService accessService,
            ChatbotCodeService codeService) {
        this.chatbotRepository = chatbotRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.accessService = accessService;
        this.codeService = codeService;
    }

    @Transactional
    public ChatbotConfigDTO create(
            @Valid CreateChatbotConfigRequest request, UUID workspaceId, UUID requesterUserId) {
        authorizationService.requireOwnerOrAdmin(workspaceId, requesterUserId);
        RequirementSet project = projectRepository.findById(
                        Objects.requireNonNull(request.getRequirementSetId()))
                .orElseThrow(() -> new ResourceNotFoundException("Projeto", request.getRequirementSetId()));
        Workspace workspace = project.getWorkspace();
        if (workspace == null || !workspace.getId().equals(workspaceId)) {
            throw new ForbiddenException("O projeto não pertence ao workspace informado.");
        }

        String rawCode = uniqueCode();
        ChatbotConfig chatbot = new ChatbotConfig(
                request.getName().trim(), codeService.hash(rawCode), project,
                request.getStartTime(), request.getEndTime());
        chatbot.setShowRequirementsToUsers(Boolean.TRUE.equals(request.getShowRequirementsToUsers()));
        return toDTO(chatbotRepository.save(chatbot), rawCode);
    }

    @Transactional(readOnly = true)
    public List<ChatbotConfigDTO> listByWorkspace(UUID workspaceId, UUID requesterUserId) {
        authorizationService.requireOwnerOrAdmin(workspaceId, requesterUserId);
        return chatbotRepository.findAllByWorkspace_Id(workspaceId).stream()
                .map(chatbot -> toDTO(chatbot, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatbotConfigDTO> listByProject(
            UUID workspaceId, UUID projectId, UUID requesterUserId) {
        RequirementSet project = authorizationService
                .requireOwnerOrAdminForProject(projectId, requesterUserId);
        if (project.getWorkspace() == null || !project.getWorkspace().getId().equals(workspaceId)) {
            throw new ResourceNotFoundException("Projeto", projectId);
        }
        return chatbotRepository.findAllByRequirementSet_Id(projectId).stream()
                .map(chatbot -> toDTO(chatbot, null))
                .toList();
    }

    @Transactional
    public ChatbotConfigDTO setActive(
            UUID workspaceId, UUID chatbotId, boolean active, UUID requesterUserId) {
        ChatbotConfig chatbot = accessService.requireManager(workspaceId, chatbotId, requesterUserId);
        chatbot.setIsActive(active);
        return toDTO(chatbotRepository.save(chatbot), null);
    }

    /** Compatibilidade com rota global bloqueada em SecurityConfig. */
    @Deprecated
    public ChatbotConfigDTO createOrUpdateConfig(CreateChatbotConfigRequest request) {
        throw new ForbiddenException("Use a rota de chatbots do workspace.");
    }

    /** Compatibilidade com rota global bloqueada em SecurityConfig. */
    @Deprecated
    @Transactional(readOnly = true)
    public ChatbotConfigDTO getActiveConfig() {
        ChatbotConfig chatbot = chatbotRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum chatbot ativo."));
        return toDTO(chatbot, null);
    }

    @Deprecated
    @Transactional(readOnly = true)
    public List<ChatbotConfigDTO> getAllConfigs() {
        return chatbotRepository.findAllWithRequirementSet().stream()
                .map(chatbot -> toDTO(chatbot, null))
                .toList();
    }

    @Deprecated
    @Transactional(readOnly = true)
    public ChatbotConfigDTO getConfigById(@NonNull UUID id) {
        return chatbotRepository.findByIdWithRequirementSet(id)
                .map(chatbot -> toDTO(chatbot, null))
                .orElseThrow(() -> new ResourceNotFoundException("Chatbot", id));
    }

    @Deprecated
    @Transactional
    public ChatbotConfigDTO toggleConfig(@NonNull UUID id, Boolean active) {
        ChatbotConfig chatbot = chatbotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chatbot", id));
        chatbot.setIsActive(active);
        return toDTO(chatbotRepository.save(chatbot), null);
    }

    @Deprecated
    @Transactional
    public void deleteConfig(@NonNull UUID id) {
        chatbotRepository.deleteById(id);
    }

    private String uniqueCode() {
        for (int attempt = 0; attempt < CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = codeService.generate();
            if (!chatbotRepository.existsByAccessCodeHash(codeService.hash(code))) {
                return code;
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único para o chatbot.");
    }

    private ChatbotConfigDTO toDTO(ChatbotConfig chatbot, String rawCode) {
        RequirementSet project = chatbot.getRequirementSet();
        Workspace workspace = chatbot.getWorkspace();
        return new ChatbotConfigDTO(
                chatbot.getId(), chatbot.getName(), rawCode, chatbot.getIsActive(),
                project != null ? project.getId() : null,
                project != null ? project.getName() : null,
                workspace != null ? workspace.getId() : null,
                workspace != null ? workspace.getName() : null,
                chatbot.getStartTime(), chatbot.getEndTime(),
                chatbot.getShowRequirementsToUsers(), chatbot.getCreatedAt(),
                chatbot.getUpdatedAt(), chatbot.isAvailableNow());
    }
}
