package pl.edu.agh.backend.infrastructure.openapi;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pl.edu.agh.backend.notifications.NotificationPayload;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PKKA Backend API")
                        .version("v1")
                        .description(
                                "REST API for the Alumni Club platform of the AGH Faculty" + " of Computer Science."))
                .servers(List.of(new Server().url("http://localhost:8080").description("Local dev")));
    }

    @Bean
    public OpenApiCustomizer pushNotificationSchemas() {
        return openApi -> ModelConverters.getInstance()
                .readAll(NotificationPayload.class)
                .forEach(openApi.getComponents()::addSchemas);
    }
}
