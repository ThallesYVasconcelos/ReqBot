package requirementsAssistantAI.requirementsAssistantAI.application.controller;

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
    public ResponseEntity<AuthResponse> loginAdmin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginAsAdmin(request.getIdToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
