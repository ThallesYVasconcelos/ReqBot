package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.ChatService;
import requirementsAssistantAI.dto.ChatMessageDTO;
import requirementsAssistantAI.dto.ChatQuestionRequest;
import requirementsAssistantAI.dto.ChatResponseDTO;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbots/{chatbotId}/chat")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatService chatService;

    public ChatbotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Enviar pergunta a um chatbot acessível")
    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDTO> askQuestion(
            @PathVariable UUID chatbotId,
            @Valid @RequestBody ChatQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.answerQuestion(
                request.getQuestion(), userId(jwt), jwt.getClaimAsString("email"), chatbotId));
    }

    @Operation(summary = "Histórico do usuário neste chatbot")
    @GetMapping("/history/me")
    public ResponseEntity<List<ChatMessageDTO>> getMyHistory(
            @PathVariable UUID chatbotId,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(chatService.getMyChatHistory(
                chatbotId, userId(jwt), jwt.getClaimAsString("email")));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
