package requirementsAssistantAI.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class ApiKeyCipher {
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final String masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCipher(@Value("${ai.credentials.encryption-key:}") String masterKey) {
        this.masterKey = masterKey;
    }

    public EncryptedValue encrypt(String plainText) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new EncryptedValue(Base64.getEncoder().encodeToString(encrypted), Base64.getEncoder().encodeToString(iv));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível cifrar a credencial de IA.", exception);
        }
    }

    public String decrypt(String encryptedValue, String ivValue) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(TAG_LENGTH, Base64.getDecoder().decode(ivValue)));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedValue)), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível ler a credencial de IA configurada.", exception);
        }
    }

    private SecretKeySpec secretKey() {
        try {
            byte[] decoded = Base64.getDecoder().decode(masterKey == null ? "" : masterKey.trim());
            if (decoded.length != 32) throw new IllegalArgumentException();
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("AI_CREDENTIALS_ENCRYPTION_KEY deve ser uma chave Base64 de 32 bytes.");
        }
    }

    public record EncryptedValue(String cipherText, String iv) { }
}
