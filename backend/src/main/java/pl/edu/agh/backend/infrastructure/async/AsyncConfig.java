package pl.edu.agh.backend.infrastructure.async;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Lets side effects such as push notifications run on Spring Boot's task executor instead of the request thread. */
@Configuration
@EnableAsync
public class AsyncConfig {}
