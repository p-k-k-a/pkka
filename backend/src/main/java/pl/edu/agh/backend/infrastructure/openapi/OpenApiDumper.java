package pl.edu.agh.backend.infrastructure.openapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class OpenApiDumper implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(OpenApiDumper.class);
    private static final Path DUMP_PATH = Path.of("openapi.json");

    @Value("${server.port:8080}")
    private int port;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        URI uri = URI.create("http://localhost:" + port + apiDocsPath);

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn(
                        "Failed to fetch OpenAPI spec from {}: HTTP {}",
                        uri,
                        response.statusCode());
                return;
            }

            Files.writeString(DUMP_PATH, response.body());
            log.info("OpenAPI spec written to {}", DUMP_PATH.toAbsolutePath());
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            log.warn("Failed to dump OpenAPI spec from {}: {}", uri, e.getMessage());
        }
    }
}
