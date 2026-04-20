package requirementsAssistantAI.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint público de health check.
 * Usado pelo UptimeRobot para manter o Render acordado (free tier dorme após 15 min).
 * Configure o UptimeRobot para fazer GET em /api/health a cada 5 minutos.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", LocalDateTime.now().toString(),
                "service", "requirements-assistant"
        ));
    }
}
