package pl.edu.agh.backend.infrastructure.discord;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class DiscordClientConfig {

    @Bean
    RestClient discordRestClient() {
        // The JDK client, because HttpURLConnection refuses PATCH, which editing a message needs.
        HttpClient httpClient =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));
        return RestClient.builder().requestFactory(requestFactory).build();
    }
}
