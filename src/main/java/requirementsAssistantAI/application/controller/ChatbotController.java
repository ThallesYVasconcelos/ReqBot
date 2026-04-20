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
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatService chatService;

    public ChatbotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Enviar pergunta ao chatbot (autenticado — histórico é salvo)")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDTO> askQuestion(
            @Valid @RequestBody ChatQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt != null ? jwt.getClaimAsString("sub") : null;
        ChatResponseDTO response = chatService.answerQuestion(request.getQuestion(), userEmail);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Histórico de perguntas do chatbot por projeto")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/project/{requirementSetId}")
    public ResponseEntity<List<ChatMessageDTO>> getHistoryByProject(
            @PathVariable UUID requirementSetId) {
        return ResponseEntity.ok(chatService.getChatHistoryByProject(requirementSetId));
    }

    @Operation(summary = "Histórico de perguntas do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/history/me")
    public ResponseEntity<List<ChatMessageDTO>> getMyHistory(
            @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("sub");
        return ResponseEntity.ok(chatService.getChatHistoryByUser(userEmail));
    }
}
