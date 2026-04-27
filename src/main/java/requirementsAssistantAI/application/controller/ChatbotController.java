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
@RequestMapping("/api/workspaces/{workspaceId}/chat")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotController {

    private final ChatService chatService;

    public ChatbotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Enviar pergunta ao chatbot do workspace (membro autenticado)")
    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDTO> askQuestion(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ChatQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(chatService.answerQuestion(request.getQuestion(), userEmail, workspaceId));
    }

    @Operation(summary = "Histórico de perguntas do usuário autenticado neste workspace")
    @GetMapping("/history/me")
    public ResponseEntity<List<ChatMessageDTO>> getMyHistory(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(chatService.getChatHistoryByUserAndWorkspace(userEmail, workspaceId));
    }

    @Operation(summary = "Histórico completo do workspace (admin/owner)")
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDTO>> getWorkspaceHistory(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(chatService.getChatHistoryByWorkspace(userEmail, workspaceId));
    }
}
