package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import requirementsAssistantAI.application.service.auth.AuthService;
import requirementsAssistantAI.dto.AuthResponse;
import requirementsAssistantAI.dto.GoogleLoginRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(
            summary = "Login com Google",
            description = "Autentica a identidade. Papéis OWNER e ADMIN são resolvidos por workspace.",
            security = {})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.login(request.getIdToken()));
    }
}
