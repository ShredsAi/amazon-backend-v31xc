package ai.shreds.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class InfrastructureResilienceConfig {
    // Resilience4j is auto-configured by spring-boot-starter
    // Spring Retry is enabled by @EnableRetry
}
