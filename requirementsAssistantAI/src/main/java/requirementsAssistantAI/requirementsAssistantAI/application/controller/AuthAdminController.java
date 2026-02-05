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
@RequestMapping("/api/auth/admin")
public class AuthAdminController {

    private final AuthService authService;

    public AuthAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(summary = "Login de administrador", description = "Autentica um administrador usando Google OAuth e retorna um token JWT. Requer email na lista de admins.", security = {})
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginAsAdmin(request.getIdToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
