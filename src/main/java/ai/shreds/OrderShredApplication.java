package ai.shreds;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJms
@EnableRetry
@EnableTransactionManagement
public class OrderShredApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderShredApplication.class, args);
    }
}