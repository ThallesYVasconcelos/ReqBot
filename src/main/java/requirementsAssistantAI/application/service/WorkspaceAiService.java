package requirementsAssistantAI.application.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.application.ports.ChatAiService;
import requirementsAssistantAI.domain.AiProvider;
import requirementsAssistantAI.domain.Workspace;
import requirementsAssistantAI.domain.WorkspaceAiConfiguration;
import requirementsAssistantAI.dto.UpdateWorkspaceAiConfigurationRequest;
import requirementsAssistantAI.dto.WorkspaceAiConfigurationDTO;
import requirementsAssistantAI.infrastructure.WorkspaceAiConfigurationRepository;
import requirementsAssistantAI.infrastructure.config.ApiKeyCipher;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkspaceAiService {
    private static final String KEY_VERSION = "v1";
    private final WorkspaceAuthorizationService authorizationService;
    private final WorkspaceAiConfigurationRepository configurationRepository;
    private final ApiKeyCipher cipher;
    private final ChatModel defaultChatModel;
    private final String openAiBaseUrl;

    public WorkspaceAiService(
            WorkspaceAuthorizationService authorizationService,
            WorkspaceAiConfigurationRepository configurationRepository,
            ApiKeyCipher cipher,
            ChatModel defaultChatModel,
            @Value("${openai.base-url:https://api.openai.com/v1}") String openAiBaseUrl) {
        this.authorizationService = authorizationService;
        this.configurationRepository = configurationRepository;
        this.cipher = cipher;
        this.defaultChatModel = defaultChatModel;
        this.openAiBaseUrl = openAiBaseUrl;
    }

    @Transactional(readOnly = true)
    public WorkspaceAiConfigurationDTO getConfiguration(UUID workspaceId, UUID requesterId) {
        authorizationService.requireOwner(workspaceId, requesterId);
        return configurationRepository.findByWorkspace_Id(workspaceId)
                .map(this::toDto)
                .orElse(new WorkspaceAiConfigurationDTO(false, null, null, null, null));
    }

    @Transactional
    public WorkspaceAiConfigurationDTO saveConfiguration(
            UUID workspaceId, UUID requesterId, UpdateWorkspaceAiConfigurationRequest request) {
        Workspace workspace = authorizationService.requireOwner(workspaceId, requesterId);
        validateModel(request.provider(), request.model());
        String apiKey = request.apiKey().trim();
        ApiKeyCipher.EncryptedValue encrypted = cipher.encrypt(apiKey);
        WorkspaceAiConfiguration configuration = configurationRepository.findByWorkspace_Id(workspaceId)
                .orElseGet(WorkspaceAiConfiguration::new);
        configuration.setWorkspace(workspace);
        configuration.setProvider(request.provider());
        configuration.setModel(request.model().trim());
        configuration.setEncryptedApiKey(encrypted.cipherText());
        configuration.setEncryptionIv(encrypted.iv());
        configuration.setKeyVersion(KEY_VERSION);
        configuration.setKeyHint(mask(apiKey));
        return toDto(configurationRepository.save(configuration));
    }

    @Transactional
    public void deleteConfiguration(UUID workspaceId, UUID requesterId) {
        authorizationService.requireOwner(workspaceId, requesterId);
        configurationRepository.findByWorkspace_Id(workspaceId).ifPresent(configurationRepository::delete);
    }

    public Map<AiProvider, List<String>> availableModels() {
        return Map.of(
                AiProvider.OPENAI, modelsFor(AiProvider.OPENAI),
                AiProvider.GEMINI, modelsFor(AiProvider.GEMINI),
                AiProvider.ANTHROPIC, modelsFor(AiProvider.ANTHROPIC));
    }

    public AssistantAiService assistant(UUID workspaceId) {
        return AiServices.builder(AssistantAiService.class).chatModel(resolveChatModel(workspaceId)).build();
    }

    public ChatAiService chat(UUID workspaceId) {
        return AiServices.builder(ChatAiService.class).chatModel(resolveChatModel(workspaceId)).build();
    }

    /** Identifica a configuração efetiva sem expor qualquer parte da credencial. */
    public String cacheFingerprint(UUID workspaceId) {
        return configurationRepository.findByWorkspace_Id(workspaceId)
                .map(config -> config.getProvider() + ":" + config.getModel() + ":" + config.getUpdatedAt())
                .orElse("server-default");
    }

    private ChatModel resolveChatModel(UUID workspaceId) {
        return configurationRepository.findByWorkspace_Id(workspaceId)
                .map(this::createModel)
                .orElse(defaultChatModel);
    }

    private ChatModel createModel(WorkspaceAiConfiguration configuration) {
        String apiKey = cipher.decrypt(configuration.getEncryptedApiKey(), configuration.getEncryptionIv());
        return switch (configuration.getProvider()) {
            case GEMINI -> GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKey).modelName(configuration.getModel()).temperature(0.3)
                    .maxOutputTokens(2048).timeout(Duration.ofSeconds(60)).build();
            case OPENAI -> OpenAiChatModel.builder()
                    .apiKey(apiKey).modelName(configuration.getModel()).baseUrl(openAiBaseUrl)
                    .temperature(0.3).maxTokens(2048).timeout(Duration.ofSeconds(60)).build();
            case ANTHROPIC -> AnthropicChatModel.builder()
                    .apiKey(apiKey).modelName(configuration.getModel()).temperature(0.3)
                    .maxTokens(2048).timeout(Duration.ofSeconds(60)).build();
        };
    }

    private void validateModel(AiProvider provider, String model) {
        if (model == null || !modelsFor(provider).contains(model.trim())) {
            throw new IllegalArgumentException("Modelo não suportado para o provedor selecionado.");
        }
    }

    private List<String> modelsFor(AiProvider provider) {
        return switch (provider) {
            case OPENAI -> List.of("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "gpt-4.1", "gpt-5-mini", "gpt-5");
            case GEMINI -> List.of("gemini-3.5-flash-lite", "gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.5-pro");
            case ANTHROPIC -> List.of("claude-haiku-4-5", "claude-sonnet-4-5", "claude-opus-4-5", "claude-sonnet-4-6", "claude-opus-4-6");
        };
    }

    private WorkspaceAiConfigurationDTO toDto(WorkspaceAiConfiguration config) {
        return new WorkspaceAiConfigurationDTO(true, config.getProvider(), config.getModel(), config.getKeyHint(), config.getUpdatedAt());
    }

    private String mask(String key) {
        return "••••" + key.substring(Math.max(0, key.length() - 4));
    }
}
