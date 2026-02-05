package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
public class RequirementSetDTO {
    private UUID id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer requirementsCount;

    public RequirementSetDTO() {
    }

    public RequirementSetDTO(UUID id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, Integer requirementsCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requirementsCount = requirementsCount;
    }

   
}
