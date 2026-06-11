package pl.edu.agh.backend.infrastructure.web;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class WebCorsConfig {

    private static final List<String> CORS_PATHS =
            List.of("/api/**", "/v3/api-docs/**", "/swagger-ui/**", "/oauth2/**", "/login/oauth2/**", "/error", "/");

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.web.cors-allowed-origins}") String allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        for (String path : CORS_PATHS) {
            source.registerCorsConfiguration(path, config);
        }
        return source;
    }
}
