package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class ChatbotScheduleDTO {
    private String startTime;
    private String endTime;
    private boolean available24h;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public ChatbotScheduleDTO() {}

    public ChatbotScheduleDTO(LocalTime startTime, LocalTime endTime) {
        if (startTime != null && endTime != null) {
            this.startTime = startTime.format(TIME_FORMAT);
            this.endTime = endTime.format(TIME_FORMAT);
            this.available24h = false;
        } else {
            this.available24h = true;
        }
    }
}
