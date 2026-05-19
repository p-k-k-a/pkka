package pl.edu.agh.backend.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("PKKA Backend API")
                                .version("v1")
                                .description(
                                        "REST API for the Alumni Club platform of the AGH Faculty"
                                                + " of Computer Science."))
                .servers(
                        List.of(
                                new Server()
                                        .url("http://localhost:8080")
                                        .description("Local dev")));
    }
}
