package requirementsAssistantAI.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme bearerJwt = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT token obtido através do endpoint /api/auth/user/google ou /api/auth/admin/google");
        
        SecurityRequirement securityRequirement = new SecurityRequirement()
                .addList("bearer-jwt");
        
        return new OpenAPI()
                .info(new Info()
                        .title("Requirements Assistant AI API")
                        .version("1.0.0")
                        .description("API para gerenciamento e refinamento de requisitos de software com IA (Gemini), chatbot RAG e login via Google")
                        .contact(new Contact()
                                .name("Requirements Assistant AI")
                                .email("support@requirements-assistant.ai"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8084").description("Servidor Local")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", bearerJwt))
                // Aplica segurança globalmente a todos os endpoints
                .addSecurityItem(securityRequirement);
    }
    
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/**")
                .build();
    }
}
