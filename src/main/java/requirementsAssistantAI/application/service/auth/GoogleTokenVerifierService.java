package requirementsAssistantAI.application.service.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import requirementsAssistantAI.application.exception.UnauthorizedException;

@Service
public class GoogleTokenVerifierService {

    private final RestClient restClient;
    private final String googleClientId;

    public GoogleTokenVerifierService(
            @Value("${auth.google.client-id:}") String googleClientId
    ) {
        this.restClient = RestClient.create();
        this.googleClientId = googleClientId == null ? "" : googleClientId.trim();
    }

    public GoogleTokenInfo verifyIdToken(String idToken) {
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new UnauthorizedException("idToken é obrigatório");
        }

        GoogleTokenInfo info;
        try {
            info = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={idToken}", idToken)
                    .retrieve()
                    .body(GoogleTokenInfo.class);
        } catch (RestClientException e) {
            throw new UnauthorizedException("Falha ao validar idToken no Google", e);
        }

        if (info == null) {
            throw new UnauthorizedException("Falha ao validar idToken no Google (resposta vazia)");
        }

        if (!info.isEmailVerifiedBoolean()) {
            throw new UnauthorizedException("Email do Google não verificado");
        }

        if (info.getEmail() == null || info.getEmail().isBlank()) {
            throw new UnauthorizedException("Email não encontrado no token do Google");
        }

        if (!googleClientId.isEmpty() && info.getAud() != null && !googleClientId.equals(info.getAud())) {
            throw new UnauthorizedException("Token do Google inválido: audience (aud) não corresponde ao client-id configurado");
        }

        return info;
    }
}
