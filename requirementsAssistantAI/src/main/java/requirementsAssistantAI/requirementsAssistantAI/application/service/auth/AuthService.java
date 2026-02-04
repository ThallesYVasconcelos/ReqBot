package requirementsAssistantAI.requirementsAssistantAI.application.service.auth;

import requirementsAssistantAI.requirementsAssistantAI.domain.AppUser;
import requirementsAssistantAI.requirementsAssistantAI.domain.AuthRole;
import requirementsAssistantAI.requirementsAssistantAI.infrastructure.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import requirementsAssistantAI.requirementsAssistantAI.application.exception.ForbiddenException;
import requirementsAssistantAI.requirementsAssistantAI.dto.AuthResponse;

@Service
public class AuthService {

    private final GoogleTokenVerifierService googleTokenVerifierService;
    private final AppUserRepository appUserRepository;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long ttlSeconds;
    private final Set<String> adminEmails;

    public AuthService(
            GoogleTokenVerifierService googleTokenVerifierService,
            AppUserRepository appUserRepository,
            JwtEncoder jwtEncoder,
            @Value("${auth.jwt.issuer:requirements-assistant-api}") String issuer,
            @Value("${auth.jwt.ttl-seconds:3600}") long ttlSeconds,
            @Value("${auth.admin.emails:}") String adminEmailsCsv
    ) {
        this.googleTokenVerifierService = googleTokenVerifierService;
        this.appUserRepository = appUserRepository;
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
        this.adminEmails = parseEmails(adminEmailsCsv);
    }

    @Transactional
    public AuthResponse loginAsUser(String idToken) {
        GoogleTokenInfo info = googleTokenVerifierService.verifyIdToken(idToken);
        AppUser user = upsertUser(info, AuthRole.USER);
        return issueToken(user.getId(), user.getEmail(), user.getRole(), AuthRole.USER);
    }

    @Transactional
    public AuthResponse loginAsAdmin(String idToken) {
        GoogleTokenInfo info = googleTokenVerifierService.verifyIdToken(idToken);
        String email = info.getEmail().toLowerCase();
        if (adminEmails.isEmpty() || !adminEmails.contains(email)) {
            throw new ForbiddenException("Acesso negado: este email não está autorizado como ADMIN");
        }
        AppUser user = upsertUser(info, AuthRole.ADMIN);
        return issueToken(user.getId(), user.getEmail(), user.getRole(), AuthRole.ADMIN);
    }

    private AppUser upsertUser(GoogleTokenInfo info, AuthRole desiredRole) {
        String email = Objects.requireNonNull(info.getEmail()).toLowerCase();

        AppUser user = appUserRepository.findByEmail(email)
                .orElseGet(() -> new AppUser(email, info.getName(), info.getPicture(), AuthRole.USER));

        user.setEmail(email);
        user.setName(info.getName());
        user.setPictureUrl(info.getPicture());

        if (user.getRole() == AuthRole.ADMIN) {
            // never downgrade
        } else {
            user.setRole(desiredRole);
        }

        return appUserRepository.save(user);
    }

    private AuthResponse issueToken(UUID userId, String email, AuthRole storedRole, AuthRole tokenRole) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", tokenRole.name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new AuthResponse(token, "Bearer", ttlSeconds, tokenRole.name());
    }

    private static Set<String> parseEmails(String csv) {
        if (csv == null || csv.trim().isEmpty()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
