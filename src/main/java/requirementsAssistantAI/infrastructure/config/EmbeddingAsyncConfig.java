package requirementsAssistantAI.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class EmbeddingAsyncConfig {

    @Bean(name = "embeddingTaskExecutor")
    public Executor embeddingTaskExecutor(
            @Value("${ai.embedding.queue-capacity:25}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(Math.max(1, Math.min(queueCapacity, 100)));
        executor.setThreadNamePrefix("embedding-");
        // Backpressure limita RAM e evita perder indexações quando a fila enche.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
