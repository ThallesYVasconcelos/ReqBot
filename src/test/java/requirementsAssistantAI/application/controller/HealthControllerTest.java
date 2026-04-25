package requirementsAssistantAI.application.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTest {

    @Test
    void healthShouldReturnUpStatusAndServiceName() {
        HealthController controller = new HealthController();

        ResponseEntity<Map<String, Object>> response = controller.health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody())
                .containsEntry("status", "UP")
                .containsEntry("service", "requirements-assistant")
                .containsKey("timestamp");
    }
}
