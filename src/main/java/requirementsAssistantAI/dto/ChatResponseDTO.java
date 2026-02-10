package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatResponseDTO {
    private String answer;
    private String question;
    private LocalDateTime timestamp;
    private Boolean success;
    private Boolean isAvailable;

    public ChatResponseDTO() {
    }

    public ChatResponseDTO(String answer, String question, LocalDateTime timestamp, Boolean success) {
        this.answer = answer;
        this.question = question;
        this.timestamp = timestamp;
        this.success = success;
        this.isAvailable = success; // isAvailable é o mesmo que success
    }

    public ChatResponseDTO(String answer, String question, LocalDateTime timestamp, Boolean success, Boolean isAvailable) {
        this.answer = answer;
        this.question = question;
        this.timestamp = timestamp;
        this.success = success;
        this.isAvailable = isAvailable;
    }
}
