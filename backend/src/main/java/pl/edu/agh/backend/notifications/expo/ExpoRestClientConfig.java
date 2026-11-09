package pl.edu.agh.backend.notifications.expo;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Configuration
public class ExpoRestClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    @Bean
    RestClient expoRestClient(
            @Value("${app.notifications.base-url}") String baseUrl,
            @Value("${app.notifications.access-token:}") String accessToken) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build());
        factory.setReadTimeout(READ_TIMEOUT);
        return configure(RestClient.builder().requestFactory(factory), baseUrl, accessToken)
                .build();
    }

    static RestClient.Builder configure(RestClient.Builder builder, String baseUrl, String accessToken) {
        RestClient.Builder configured = builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate");
        if (StringUtils.hasText(accessToken)) {
            configured = configured.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        }
        return configured;
    }
}
