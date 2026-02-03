package requirementsAssistantAI.requirementsAssistantAI.application.service.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenInfo {
    private String aud;
    private String email;
    @JsonProperty("email_verified")
    private String emailVerified;
    private String name;
    private String picture;
    private String sub;
    private String iss;
    private String exp;

    public boolean isEmailVerifiedBoolean() {
        return "true".equalsIgnoreCase(emailVerified);
    }
}
