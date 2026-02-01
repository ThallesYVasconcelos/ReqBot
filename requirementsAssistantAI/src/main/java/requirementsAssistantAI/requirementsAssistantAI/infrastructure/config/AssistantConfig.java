package requirementsAssistantAI.requirementsAssistantAI.infrastructure.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import requirementsAssistantAI.requirementsAssistantAI.application.ports.AssistantAiService;
import requirementsAssistantAI.requirementsAssistantAI.application.ports.ChatAiService;

/**
 * Configuração polimórfica do assistente de IA.
 * Usa @ConditionalOnProperty para ativar o provedor escolhido em ai.provider.
 */
@Configuration
public class AssistantConfig {

    @Value("${gemini.api-key:}") private String geminiApiKey;
    @Value("${gemini.model:gemini-1.5-flash}") private String geminiModel;

    @Value("${openai.api-key:}") private String openAiApiKey;
    @Value("${openai.model:gpt-4o}") private String openAiModel;

    @Value("${ollama.base-url:http://localhost:11434}") private String ollamaUrl;
    @Value("${ollama.model:llama3}") private String ollamaModel;

    @Value("${pgvector.host:localhost}") private String pgHost;
    @Value("${pgvector.port:5432}") private int pgPort;
    @Value("${pgvector.database:postgres}") private String pgDatabase;
    @Value("${pgvector.user:postgres}") private String pgUser;
    @Value("${pgvector.password:}") private String pgPassword;

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "gemini", matchIfMissing = true)
    public ChatModel geminiModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName(geminiModel)
                .temperature(0.3)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
    public ChatModel openAiModel() {
        return OpenAiChatModel.builder()
                .apiKey(openAiApiKey)
                .modelName(openAiModel)
                .temperature(0.3)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "ai.provider", havingValue = "ollama")
    public ChatModel ollamaModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaUrl)
                .modelName(ollamaModel)
                .temperature(0.3)
                .build();
    }

  
    @Bean
    public EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel(); 
    }

   
    @Bean
    @Profile("!test")
    public EmbeddingStore<TextSegment> embeddingStore() {
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
    public AssistantAiService assistantAiService(ChatModel model, AssistantTools tools) {
        return AiServices.builder(AssistantAiService.class)
                .chatModel(model)
                .tools(tools)
                .build();
    }

    @Bean
    public ChatAiService chatAiService(ChatModel model) {
        return AiServices.builder(ChatAiService.class)
                .chatModel(model)
                .build();
    }
}
