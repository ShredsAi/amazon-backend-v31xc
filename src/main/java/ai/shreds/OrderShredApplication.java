package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "ai.shreds.infrastructure.repositories")
@EntityScan(basePackages = "ai.shreds.domain.entities")
@EnableJms
@EnableRetry
@EnableAsync
@EnableTransactionManagement
public class OrderShredApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderShredApplication.class, args);
    }
}