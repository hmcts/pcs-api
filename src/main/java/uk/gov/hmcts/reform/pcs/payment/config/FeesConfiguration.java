package uk.gov.hmcts.reform.pcs.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@EnableConfigurationProperties
@ConfigurationProperties("fees-register")
public class FeesConfiguration {

    private final Map<String, LookUpReferenceData> fees = new HashMap<>();

    @Data
    public static class LookUpReferenceData {
        private String channel;
        private String event;
        private String jurisdiction1;
        private String jurisdiction2;
        private String keyword;
        private String service;
    }
}
