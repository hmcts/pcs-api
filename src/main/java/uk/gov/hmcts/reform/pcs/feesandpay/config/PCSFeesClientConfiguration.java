package uk.gov.hmcts.reform.pcs.feesandpay.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PCSFeesClientConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "fee.api")
    public FeesProperties claimFeesProperties() {
        return new FeesProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "fees.enforcement")
    public FeesProperties enforcementFeesProperties() {
        return new FeesProperties();
    }

    @Bean
    public Jurisdictions feesJurisdictions(@Qualifier("claimFeesProperties") FeesProperties properties) {
        return properties.toJurisdictions();
    }

    @Bean
    public Jurisdictions enforcementJurisdictions(@Qualifier("enforcementFeesProperties") FeesProperties properties) {
        return properties.toJurisdictions();
    }

    @Bean
    public ServiceName feesServiceName(@Qualifier("claimFeesProperties") FeesProperties properties) {
        return properties.toServiceName();
    }

    @Bean
    public ServiceName enforcementServiceName(@Qualifier("enforcementFeesProperties") FeesProperties properties) {
        return properties.toServiceName();
    }

    @Data
    public static class FeesProperties {
        private String jurisdiction1;
        private String jurisdiction2;
        private String service;

        public Jurisdictions toJurisdictions() {
            return new Jurisdictions(jurisdiction1, jurisdiction2);
        }

        public ServiceName toServiceName() {
            return new ServiceName(service);
        }
    }

}
