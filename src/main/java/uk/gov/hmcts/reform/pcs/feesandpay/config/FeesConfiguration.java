package uk.gov.hmcts.reform.pcs.feesandpay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties("fees-register")
public class FeesConfiguration {

    private Api api = new Api();
    private Map<String, LookUpReferenceData> fees = new HashMap<>();

    @Data
    public static class Api {
        private String url;
    }

    @Data
    public static class LookUpReferenceData {
        private String service;
        private String jurisdiction1;
        private String jurisdiction2;
        private String channel;
        private String event;
        private String applicantType;
        private String amountOrVolume;
        private String keyword;
    }
}
