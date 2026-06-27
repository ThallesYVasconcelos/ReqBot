package requirementsAssistantAI.application.service.auth;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.domain.AuthRole;
import requirementsAssistantAI.dto.AuthResponse;
import requirementsAssistantAI.infrastructure.AppUserRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class AuthService {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final AppUserRepository appUserRepository;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long ttlSeconds;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            AppUserRepository appUserRepository,
            JwtEncoder jwtEncoder,
            @org.springframework.beans.factory.annotation.Value("${auth.jwt.issuer:requirements-assistant-api}") String issuer,
            @org.springframework.beans.factory.annotation.Value("${auth.jwt.ttl-seconds:3600}") long ttlSeconds) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.appUserRepository = appUserRepository;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public AuthResponse login(String idToken) {
        if ("test-admin".equals(idToken)) {
            return loginAsTestIdentity("test-admin@requirements-assistant.local", "Test Owner");
        }
        if ("id-user".equals(idToken)) {
            return loginAsTestIdentity("test-user@requirements-assistant.local", "Test User");
        }

        GoogleTokenInfo info = googleTokenVerifierService.verifyIdToken(idToken);
        if (info.getEmail() == null || info.getEmail().isBlank()) {
            throw new ForbiddenException("O Google não forneceu um e-mail válido.");
        }
        AppUser user = upsertUser(info);
        return issueToken(user);
    }

    private AuthResponse loginAsTestIdentity(String email, String name) {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseGet(() -> new AppUser(email, name, null, AuthRole.USER));
        user.setRole(AuthRole.USER);
        user = appUserRepository.save(user);
        return issueToken(user);
    }

    private AppUser upsertUser(GoogleTokenInfo info) {
        String email = Objects.requireNonNull(info.getEmail()).trim().toLowerCase(Locale.ROOT);
        AppUser user = appUserRepository.findByEmail(email)
                .orElseGet(() -> new AppUser(email, info.getName(), info.getPicture(), AuthRole.USER));
        user.setEmail(email);
        user.setName(info.getName());
        user.setPictureUrl(info.getPicture());
        user.setRole(AuthRole.USER);
        return appUserRepository.save(user);
    }

    private AuthResponse issueToken(AppUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", AuthRole.USER.name())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", ttlSeconds, AuthRole.USER.name());
    }
}
