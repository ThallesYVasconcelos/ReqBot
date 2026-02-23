package requirementsAssistantAI.application.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "message", "Erro de validação nos campos obrigatórios",
                        "errors", errors
                ));
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Erro interno no servidor: {}", ex.getMessage(), ex);
        String userMessage = "Erro interno do servidor. Tente novamente em alguns minutos.";
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("connection") || ex.getMessage().contains("database") || ex.getMessage().contains("Connection") || ex.getMessage().contains("SQL")) {
                userMessage = "Serviço temporariamente indisponível. O banco de dados está em manutenção. Tente novamente em alguns minutos.";
            } else if (ex.getMessage().contains("token") || ex.getMessage().contains("Token") || ex.getMessage().contains("Google")) {
                userMessage = "Erro ao validar credenciais do Google. Tente fazer login novamente.";
            }
        }
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", userMessage));
    }
}
