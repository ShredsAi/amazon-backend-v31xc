package ai.shreds.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "ai.shreds.infrastructure.repositories")
public class InfrastructureDatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(InfrastructureDatabaseConfig.class);

    @Autowired
    private Environment environment;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(environment.getProperty("spring.datasource.url"));
        config.setUsername(environment.getProperty("spring.datasource.username"));
        config.setPassword(environment.getProperty("spring.datasource.password"));
        config.setDriverClassName(environment.getProperty(
                "spring.datasource.driver-class-name",
                "com.mysql.cj.jdbc.Driver"
        ));

        // Connection pool settings
        config.setMaximumPoolSize(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 10));
        config.setMinimumIdle(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
        config.setIdleTimeout(environment.getProperty("spring.datasource.hikari.idle-timeout", Long.class, 300000L));
        config.setConnectionTimeout(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 20000L));
        config.setMaxLifetime(environment.getProperty("spring.datasource.hikari.max-lifetime", Long.class, 1200000L));

        // Performance settings
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        // Connection testing
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(3000);

        HikariDataSource dataSource = new HikariDataSource(config);
        logger.info("Configured HikariCP connection pool with maximum size: {}", config.getMaximumPoolSize());
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("ai.shreds.domain.entities");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(environment.getProperty("spring.jpa.show-sql", Boolean.class, false));
        vendorAdapter.setGenerateDdl(environment.getProperty("spring.jpa.generate-ddl", Boolean.class, false));
        vendorAdapter.setDatabasePlatform(environment.getProperty(
                "spring.jpa.database-platform",
                "org.hibernate.dialect.MySQL8Dialect"
        ));
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties jpaProperties = new Properties();
        // Hibernate settings
        jpaProperties.setProperty("hibernate.hbm2ddl.auto",
                environment.getProperty("spring.jpa.hibernate.ddl-auto", "none"));
        jpaProperties.setProperty("hibernate.dialect",
                environment.getProperty("spring.jpa.properties.hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect"));
        
        // Query settings
        jpaProperties.setProperty("hibernate.jdbc.batch_size",
                environment.getProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "50"));
        jpaProperties.setProperty("hibernate.order_inserts",
                environment.getProperty("spring.jpa.properties.hibernate.order_inserts", "true"));
        jpaProperties.setProperty("hibernate.order_updates",
                environment.getProperty("spring.jpa.properties.hibernate.order_updates", "true"));
        
        // Cache settings
        jpaProperties.setProperty("hibernate.cache.use_second_level_cache",
                environment.getProperty("spring.jpa.properties.hibernate.cache.use_second_level_cache", "false"));
        
        // Statistics and logging
        jpaProperties.setProperty("hibernate.generate_statistics",
                environment.getProperty("spring.jpa.properties.hibernate.generate_statistics", "false"));
        jpaProperties.setProperty("hibernate.format_sql",
                environment.getProperty("spring.jpa.properties.hibernate.format_sql", "true"));

        emf.setJpaProperties(jpaProperties);
        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}