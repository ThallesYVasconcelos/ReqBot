package requirementsAssistantAI.requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String tokenType, long expiresIn, String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.role = role;
    }
}
