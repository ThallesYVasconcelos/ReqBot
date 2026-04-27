package requirementsAssistantAI.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

    @Value("${openai.api.key:${openai.api-key:}}") private String openAiApiKey;
    @Value("${openai.model:gpt-4o-mini}") private String openAiModel;
    @Value("${openai.base-url:https://api.openai.com/v1}") private String openAiBaseUrl;

    @Value("${ai.embedding.model:gemini-embedding-001}") private String embeddingModelName;
    @Value("${ai.embedding.dimension:768}") private int embeddingDimension;

    @Value("${pgvector.host:localhost}") private String pgHost;
    @Value("${pgvector.port:5432}") private int pgPort;
    @Value("${pgvector.database:postgres}") private String pgDatabase;
    @Value("${pgvector.user:postgres}") private String pgUser;
    @Value("${pgvector.password:}") private String pgPassword;

    @Bean
    public ChatModel chatModel() {
        String geminiKey = trim(geminiApiKey);
        String openAiKey = trim(openAiApiKey);

        if (!geminiKey.isBlank()) {
            log.info("Provedor de IA: Gemini (modelo: {})", geminiModel);
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiKey)
                    .modelName(geminiModel)
                    .temperature(0.3)
                    .maxOutputTokens(2048)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        if (!openAiKey.isBlank()) {
            log.info("Provedor de IA: OpenAI (modelo: {}, base-url: {})", openAiModel, openAiBaseUrl);
            return OpenAiChatModel.builder()
                    .apiKey(openAiKey)
                    .modelName(openAiModel)
                    .baseUrl(openAiBaseUrl)
                    .temperature(0.3)
                    .maxTokens(2048)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        log.warn("Nenhuma API key de IA configurada (GEMINI_API_KEY ou OPENAI_API_KEY). " +
                 "Chamadas à IA falharão. Configure pelo menos uma das variáveis de ambiente.");
        return GoogleAiGeminiChatModel.builder()
                .apiKey("")
                .modelName(geminiModel)
                .temperature(0.3)
                .maxOutputTokens(2048)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @Lazy
    public EmbeddingModel embeddingModel() {
        String geminiKey = trim(geminiApiKey);
        if (geminiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY (ou gemini.api-key) é obrigatória para embeddings via API Gemini. " +
                    "Obtenha uma chave em https://aistudio.google.com/apikey e aplique a migração SQL " +
                    "supabase/migrations/20250427120000_embeddings_vector_768_gemini.sql no Postgres " +
                    "(coluna embeddings.embedding deve ser vector(" + embeddingDimension + ")).");
        }
        log.info("Embeddings: Gemini API (modelo: {}, dimensão: {})", embeddingModelName, embeddingDimension);
        return GoogleAiEmbeddingModel.builder()
                .apiKey(geminiKey)
                .modelName(embeddingModelName)
                .outputDimensionality(embeddingDimension)
                .taskType(GoogleAiEmbeddingModel.TaskType.SEMANTIC_SIMILARITY)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Bean
    @Profile("!test")
    @Lazy
    public EmbeddingStore<TextSegment> embeddingStore(@Autowired(required = false) DataSource dataSource) {
        if (dataSource != null) {
            return PgVectorEmbeddingStore.datasourceBuilder()
                    .datasource(dataSource)
                    .table("embeddings")
                    .dimension(embeddingDimension)
                    .useIndex(true)
                    .indexListSize(100)
                    .build();
        }
        return PgVectorEmbeddingStore.builder()
                .host(pgHost)
                .port(pgPort)
                .database(pgDatabase)
                .user(pgUser)
                .password(pgPassword)
                .table("embeddings")
                .dimension(embeddingDimension)
                .useIndex(true)
                .indexListSize(100)
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

    private String trim(String value) {
        return value != null ? value.trim() : "";
    }
}
