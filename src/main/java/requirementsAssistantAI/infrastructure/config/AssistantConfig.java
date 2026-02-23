package requirementsAssistantAI.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.application.ports.ChatAiService;

@Configuration
public class AssistantConfig {

    private static final Logger log = LoggerFactory.getLogger(AssistantConfig.class);

    @Value("${gemini.api.key:${gemini.api-key:}}") private String geminiApiKey;
    @Value("${gemini.model:gemini-2.5-flash}") private String geminiModel;

    @Value("${pgvector.host:localhost}") private String pgHost;
    @Value("${pgvector.port:5432}") private int pgPort;
    @Value("${pgvector.database:postgres}") private String pgDatabase;
    @Value("${pgvector.user:postgres}") private String pgUser;
    @Value("${pgvector.password:}") private String pgPassword;

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
    public ChatModel geminiModel() {
        String key = geminiApiKey != null ? geminiApiKey.trim() : "";
        if (key.isBlank()) {
            log.warn("GEMINI_API_KEY está vazia - chamadas à IA falharão. Configure o secret no GitHub (Settings > Secrets > GEMINI_API_KEY) ou a variável de ambiente.");
        }
        return GoogleAiGeminiChatModel.builder()
                .apiKey(key)
                .modelName(geminiModel)
                .temperature(0.3)
                .maxOutputTokens(8192)
                .timeout(Duration.ofSeconds(90))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel(); 
    }

   
    @Bean
    @Profile("!test")
    public EmbeddingStore<TextSegment> embeddingStore(@Autowired(required = false) DataSource dataSource) {
        if (dataSource != null) {
            return PgVectorEmbeddingStore.datasourceBuilder()
                    .datasource(dataSource)
                    .table("embeddings")
                    .dimension(384)
                    .build();
        }
        return PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDatabase)
                .user(pgUser)
                .password(pgPassword)
                .table("embeddings")
                .dimension(384)
                .build();
    }

 
 
    @Bean
    @Profile("test")
    public EmbeddingStore<TextSegment> embeddingStoreInMemory() {
        return new InMemoryEmbeddingStore<>();
    }

    @Bean
    public AssistantAiService assistantAiService(ChatModel model) {
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .build();
    }

    @Bean
    public ChatAiService chatAiService(ChatModel model) {
        return AiServices.builder(ChatAiService.class)
                .chatModel(model)
                .build();
    }
}
