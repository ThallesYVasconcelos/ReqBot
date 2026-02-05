package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.requirementsAssistantAI.application.service.auth.AuthService;
import requirementsAssistantAI.requirementsAssistantAI.dto.AuthResponse;
import requirementsAssistantAI.requirementsAssistantAI.dto.GoogleLoginRequest;

@RestController
@RequestMapping("/api/auth/user")
public class AuthUserController {

    private final AuthService authService;

    public AuthUserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(summary = "Login de usuário", description = "Autentica um usuário usando Google OAuth e retorna um token JWT", security = {})
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginAsUser(request.getIdToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
