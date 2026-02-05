package requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import requirementsAssistantAI.application.service.ChatbotConfigService;
import requirementsAssistantAI.dto.ChatbotConfigDTO;
import requirementsAssistantAI.dto.CreateChatbotConfigRequest;

@RestController
@RequestMapping("/api/admin/chatbot")
public class ChatbotAdminController {

    private final ChatbotConfigService chatbotConfigService;

    public ChatbotAdminController(ChatbotConfigService chatbotConfigService) {
        this.chatbotConfigService = chatbotConfigService;
    }

    @PostMapping("/config")
    public ResponseEntity<ChatbotConfigDTO> createOrUpdateConfig(@Valid @RequestBody CreateChatbotConfigRequest request) {
        ChatbotConfigDTO config = chatbotConfigService.createOrUpdateConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(config);
    }

    @GetMapping("/config/active")
    public ResponseEntity<ChatbotConfigDTO> getActiveConfig() {
        ChatbotConfigDTO config = chatbotConfigService.getActiveConfig();
        return ResponseEntity.ok(config);
    }

    @GetMapping("/config")
    public ResponseEntity<List<ChatbotConfigDTO>> getAllConfigs() {
        List<ChatbotConfigDTO> configs = chatbotConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/config/{id}")
    public ResponseEntity<ChatbotConfigDTO> getConfigById(@PathVariable @NonNull UUID id) {
        ChatbotConfigDTO config = chatbotConfigService.getConfigById(Objects.requireNonNull(id));
        return ResponseEntity.ok(config);
    }

    @PatchMapping("/config/{id}/toggle")
    public ResponseEntity<ChatbotConfigDTO> toggleConfig(
            @PathVariable @NonNull UUID id,
            @RequestParam Boolean isActive) {
        ChatbotConfigDTO config = chatbotConfigService.toggleConfig(Objects.requireNonNull(id), isActive);
        return ResponseEntity.ok(config);
    }

    @DeleteMapping("/config/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable @NonNull UUID id) {
        chatbotConfigService.deleteConfig(Objects.requireNonNull(id));
        return ResponseEntity.noContent().build();
    }
}
