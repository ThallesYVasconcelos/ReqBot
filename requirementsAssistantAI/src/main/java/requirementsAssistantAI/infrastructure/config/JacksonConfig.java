package requirementsAssistantAI.infrastructure.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;

@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Registra o JavaTimeModule mas sobrescreve o deserializador de LocalTime
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        mapper.registerModule(javaTimeModule);
        // Registra nosso deserializador customizado (tem prioridade sobre o do JavaTimeModule)
        SimpleModule customModule = new SimpleModule("CustomLocalTimeModule");
        customModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        mapper.registerModule(customModule);
        return mapper;
    }
    
    @Bean
    @SuppressWarnings("deprecation")
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.modules(new JavaTimeModule());
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        builder.modules(module);
        return builder;
    }
    
    @Override
    public void extendMessageConverters(List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        for (org.springframework.http.converter.HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                ((MappingJackson2HttpMessageConverter) converter).setObjectMapper(objectMapper());
                break;
            }
        }
    }

    public static class LocalTimeDeserializer extends JsonDeserializer<LocalTime> {
        @Override
        public LocalTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            
            // Se for string, tenta parsear como LocalTime
            if (node.isTextual()) {
                String text = node.asText();
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                // Aceita formatos: "HH:mm", "HH:mm:ss", "HH:mm:ss.SSS"
                return LocalTime.parse(text);
            }
            
            // Se for objeto, extrai hour, minute, second, nano
            if (node.isObject()) {
                int hour = node.has("hour") ? node.get("hour").asInt(0) : 0;
                int minute = node.has("minute") ? node.get("minute").asInt(0) : 0;
                int second = node.has("second") ? node.get("second").asInt(0) : 0;
                int nano = node.has("nano") ? node.get("nano").asInt(0) : 0;
                
                return LocalTime.of(hour, minute, second, nano);
            }
            
            throw new IOException("Não foi possível deserializar LocalTime. Esperado string (HH:mm) ou objeto {hour, minute, second, nano}");
        }
    }
}
