package uk.gov.hmcts.reform.pcs.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;

@Configuration
@Order(2)
public class EntityManagerConfig {

    @Value("${spring.jpa.packages-to-scan:uk.gov.hmcts.reform.pcs}")
    private String packagesToScan;

    private final JpaPropertiesConfig jpaPropertiesConfig;

    public EntityManagerConfig(JpaPropertiesConfig jpaPropertiesConfig) {
        this.jpaPropertiesConfig = jpaPropertiesConfig;
    }

    @Bean
    @Lazy
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan(packagesToScan);
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setGenerateDdl(Boolean.parseBoolean(jpaPropertiesConfig.getHibernateGenerateDdl()));
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaProperties(jpaPropertiesConfig.toProperties());
        return em;
    }

    @Bean(name = "transactionManager")
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

}

