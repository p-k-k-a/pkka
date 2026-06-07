package pl.edu.agh.backend.infrastructure.persistence;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class JpaConfig {
    // For mocking
    @Bean
    public Clock clock() {
        return Clock.system(ZoneOffset.UTC);
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(ZonedDateTime.now(clock));
    }
}
