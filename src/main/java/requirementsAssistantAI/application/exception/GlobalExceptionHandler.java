package requirementsAssistantAI.application.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("chatbot_config")) {
            message = "Não é possível excluir este projeto pois ele está vinculado à configuração do chatbot. " +
                    "Remova ou altere a configuração do chatbot na aba Chatbot antes de excluir este projeto.";
        } else if (message != null && message.contains("foreign key")) {
            message = "Não é possível excluir pois este item está em uso por outros registros.";
        } else {
            message = "Operação não permitida: conflito com dados existentes.";
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", message));
    }
}
