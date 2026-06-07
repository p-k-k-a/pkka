package pl.edu.agh.backend.infrastructure.persistence;

import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {
    // For mocking
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }
}
