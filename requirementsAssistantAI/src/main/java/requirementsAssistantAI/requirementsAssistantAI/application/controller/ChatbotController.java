package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.requirementsAssistantAI.application.service.ChatService;
import requirementsAssistantAI.requirementsAssistantAI.dto.ChatQuestionRequest;
import requirementsAssistantAI.requirementsAssistantAI.dto.ChatResponseDTO;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatService chatService;

    public ChatbotController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponseDTO> askQuestion(@Valid @RequestBody ChatQuestionRequest request) {
        ChatResponseDTO response = chatService.answerQuestion(request.getQuestion());
        return ResponseEntity.ok(response);
    }
}
