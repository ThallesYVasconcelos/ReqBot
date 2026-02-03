package requirementsAssistantAI.requirementsAssistantAI.application.controller;

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
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginAsUser(request.getIdToken());
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
