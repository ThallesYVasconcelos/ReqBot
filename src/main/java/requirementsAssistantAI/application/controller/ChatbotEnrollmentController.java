package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.application.service.ChatbotEnrollmentService;
import requirementsAssistantAI.dto.ChatbotEnrollmentDTO;
import requirementsAssistantAI.dto.JoinChatbotRequest;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/chatbots")
@SecurityRequirement(name = "bearerAuth")
public class ChatbotEnrollmentController {

    private final ChatbotEnrollmentService enrollmentService;

    public ChatbotEnrollmentController(ChatbotEnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "Entrar em um chatbot usando o código")
    @PostMapping("/join")
    public ResponseEntity<ChatbotEnrollmentDTO> join(
            @Valid @RequestBody JoinChatbotRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(enrollmentService.join(request.code(), userId(jwt)));
    }

    @Operation(summary = "Listar chatbots em que o usuário entrou")
    @GetMapping("/me")
    public ResponseEntity<List<ChatbotEnrollmentDTO>> listMine(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(enrollmentService.listMine(userId(jwt)));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
