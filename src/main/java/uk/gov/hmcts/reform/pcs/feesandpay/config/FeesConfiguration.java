package uk.gov.hmcts.reform.pcs.feesandpay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties("fees")
public class FeesConfiguration {

    private Map<String, LookUpReferenceData> lookup = new HashMap<>();

    @Data
    public static class LookUpReferenceData {
        private String service;
        private String jurisdiction1;
        private String jurisdiction2;
        private String channel;
        private String event;
        private String applicantType;
        private BigDecimal amountOrVolume;
        private String keyword;
    }

    public LookUpReferenceData getLookup(FeeType feeType) {
        return lookup.get(feeType.getCode());
    }

}
