package requirementsAssistantAI.application.controller;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import requirementsAssistantAI.application.service.auth.AuthService;
import requirementsAssistantAI.dto.AuthResponse;
import requirementsAssistantAI.dto.GoogleLoginRequest;

@RestController
@RequestMapping("/api/auth/admin")
public class AuthAdminController {

    private final AuthService authService;

    public AuthAdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    @Operation(summary = "Login legado", description = "Compatibilidade: autentica a identidade; ADMIN é definido por workspace.", security = {})
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.login(request.getIdToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
