package requirementsAssistantAI.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${auth.jwt.secret:}")
    private String jwtSecret;

    @Bean
    public SecretKey jwtSecretKey() {
        String secret = jwtSecret != null && !jwtSecret.isBlank()
                ? jwtSecret
                : "default-secret-change-in-production-at-least-32-characters-long";
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder, CorsConfigurationSource corsConfigurationSource) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthorityPrefix("ROLE_");
        authoritiesConverter.setAuthoritiesClaimName("role");
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        http.securityMatcher("/api/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Admin global: gerencia chatbot config legado
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Workspaces: só ADMIN cria; aluno pode fazer JOIN e listar os seus
                        .requestMatchers(HttpMethod.POST, "/api/workspaces").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/workspaces/join").authenticated()
                        .requestMatchers("/api/workspaces/**").authenticated()
                        // Chat por workspace: qualquer membro autenticado
                        .requestMatchers("/api/workspaces/*/chat/**").authenticated()
                        // Requirements: só ADMIN cria/edita; autenticado pode listar
                        .requestMatchers(HttpMethod.POST, "/api/requirements").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/requirements/refine").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/requirements/save").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/requirements/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/requirements/**").hasRole("ADMIN")
                        .requestMatchers("/api/requirements/*/history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/requirements/**").authenticated()
                        // RequirementSets por workspace — todo controlo é no service via ownerEmail
                        .requestMatchers("/api/workspaces/*/requirement-sets/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/requirement-sets").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/requirement-sets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/requirement-sets/**").authenticated()
                        .requestMatchers("/api/user/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            String json = String.format(
                                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"Forbidden\",\"message\":\"Acesso negado: você não tem permissão para acessar este recurso\",\"path\":\"%s\"}",
                                    java.time.Instant.now().toString(),
                                    request.getRequestURI()
                            );
                            response.getWriter().write(json);
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            String json = String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token de autenticação inválido ou ausente\",\"path\":\"%s\"}",
                                    java.time.Instant.now().toString(),
                                    request.getRequestURI()
                            );
                            response.getWriter().write(json);
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        .anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
