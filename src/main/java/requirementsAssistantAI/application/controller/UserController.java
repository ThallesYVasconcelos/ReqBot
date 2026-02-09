package requirementsAssistantAI.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import requirementsAssistantAI.application.service.UserService;
import requirementsAssistantAI.dto.AppUserDTO;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<AppUserDTO>> getAllUsers() {
        List<AppUserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
}
