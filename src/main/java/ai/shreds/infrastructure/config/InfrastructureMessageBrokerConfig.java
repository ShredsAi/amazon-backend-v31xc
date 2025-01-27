package ai.shreds.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQPrefetchPolicy;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Queue;
import org.apache.activemq.command.ActiveMQQueue;

@Slf4j
@Configuration
@EnableJms
public class InfrastructureMessageBrokerConfig {

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user:admin}")
    private String username;

    @Value("${spring.activemq.password:admin}")
    private String password;

    @Value("${spring.activemq.pool.max-connections:10}")
    private int maxConnections;

    @Value("${spring.activemq.pool.idle-timeout:30000}")
    private int idleTimeout;

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
        factory.setBrokerURL(brokerUrl);
        factory.setUserName(username);
        factory.setPassword(password);
        factory.setTrustAllPackages(false);
        factory.setTrustedPackages(java.util.Arrays.asList("ai.shreds"));

        // Configure prefetch
        ActiveMQPrefetchPolicy prefetchPolicy = new ActiveMQPrefetchPolicy();
        prefetchPolicy.setQueuePrefetch(1);
        factory.setPrefetchPolicy(prefetchPolicy);

        // Configure redelivery
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setInitialRedeliveryDelay(1000);
        redeliveryPolicy.setBackOffMultiplier(2);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setMaximumRedeliveries(3);
        factory.setRedeliveryPolicy(redeliveryPolicy);

        return factory;
    }

    @Bean
    public ConnectionFactory connectionFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {
        PooledConnectionFactory pooledFactory = new PooledConnectionFactory();
        pooledFactory.setConnectionFactory(activeMQConnectionFactory);
        pooledFactory.setMaxConnections(maxConnections);
        pooledFactory.setIdleTimeout(idleTimeout);

        CachingConnectionFactory cachingFactory = new CachingConnectionFactory();
        cachingFactory.setTargetConnectionFactory(pooledFactory);
        cachingFactory.setSessionCacheSize(maxConnections);
        cachingFactory.setCacheProducers(true);
        cachingFactory.setCacheConsumers(true);

        log.info("Configured ActiveMQ connection factory with max connections: {}", maxConnections);
        return cachingFactory;
    }

    @Bean
    public MessageConverter jacksonJmsMessageConverter(ObjectMapper objectMapper) {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setDeliveryMode(DeliveryMode.PERSISTENT);
        template.setExplicitQosEnabled(true);
        template.setTimeToLive(3600000); // 1 hour
        template.setReceiveTimeout(2000);
        return template;
    }

    @Bean(name = "orderCreatedQueue")
    public Queue orderCreatedQueue() {
        return new ActiveMQQueue("ORDER_CREATED");
    }

    @Bean(name = "orderCancelledQueue")
    public Queue orderCancelledQueue() {
        return new ActiveMQQueue("ORDER_CANCELLED");
    }

    @Bean
    public JmsListenerContainerFactory<?> jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter
    ) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDestinationResolver(new DynamicDestinationResolver());
        
        // Concurrency settings
        factory.setConcurrency("3-10"); // min-max consumers
        factory.setSessionTransacted(true);
        
        // Error handling
        factory.setErrorHandler(t -> {
            log.error("Error in message processing", t);
        });
        
        // Recovery settings
        factory.setRecoveryInterval(5000L);
        factory.setSessionAcknowledgeMode(javax.jms.Session.CLIENT_ACKNOWLEDGE);
        
        log.info("Configured JMS listener container factory with concurrency: {}", factory.getConcurrency());
        return factory;
    }
}
