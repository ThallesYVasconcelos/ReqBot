package requirementsAssistantAI.requirementsAssistantAI.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import requirementsAssistantAI.requirementsAssistantAI.application.ports.AssistantAiService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/ai")
public class AITestController {

    private final AssistantAiService assistantAiService;

    public AITestController(AssistantAiService assistantAiService) {
        this.assistantAiService = assistantAiService;
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testAI(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        String rawRequirement = request.getOrDefault("requirement", "teste simples");
        String context = request.getOrDefault("context", "Nenhum contexto disponível");
        
        try {
            response.put("status", "success");
            response.put("input", rawRequirement);
            response.put("context", context);
            
            long startTime = System.currentTimeMillis();
            String aiResponse = assistantAiService.refineRequirement(rawRequirement, context);
            long endTime = System.currentTimeMillis();
            
            response.put("aiResponse", aiResponse);
            response.put("responseTime", (endTime - startTime) + " ms");
            response.put("responseLength", aiResponse != null ? aiResponse.length() : 0);
            response.put("isNull", aiResponse == null);
            response.put("isBlank", aiResponse != null && aiResponse.isBlank());
            
            if (aiResponse != null && aiResponse.length() > 500) {
                response.put("preview", aiResponse.substring(0, 500) + "...");
            } else {
                response.put("preview", aiResponse);
            }
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getClass().getName());
            response.put("message", e.getMessage());
            if (e.getCause() != null) {
                response.put("cause", e.getCause().getMessage());
            }
            response.put("stackTrace", e.getStackTrace());
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/simple")
    public ResponseEntity<Map<String, Object>> testSimple() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String testInput = "quero poder testar minha api";
            String testContext = "Nenhum contexto disponível";
            
            response.put("testInput", testInput);
            response.put("testContext", testContext);
            
            long startTime = System.currentTimeMillis();
            String aiResponse = assistantAiService.refineRequirement(testInput, testContext);
            long endTime = System.currentTimeMillis();
            
            response.put("status", "success");
            response.put("aiResponse", aiResponse);
            response.put("responseTime", (endTime - startTime) + " ms");
            response.put("responseLength", aiResponse != null ? aiResponse.length() : 0);
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("error", e.getClass().getName());
            response.put("message", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}
