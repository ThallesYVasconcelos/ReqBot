package requirementsAssistantAI.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String role;
    private AppUserDTO user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String tokenType, long expiresIn, String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.role = role;
    }

    public AuthResponse(String accessToken, String tokenType, long expiresIn, String role, AppUserDTO user) {
        this(accessToken, tokenType, expiresIn, role);
        this.user = user;
    }
}
