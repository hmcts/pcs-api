package uk.gov.hmcts.reform.pcs.bulkclaim.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.BulkClaimV2Handler;
import uk.gov.hmcts.reform.pcs.bulkclaim.service.NotImplementedBulkClaimV2Handler;

@Configuration
public class BulkClaimHandlerConfiguration {

    @Bean
    @ConditionalOnMissingBean(BulkClaimV2Handler.class)
    public BulkClaimV2Handler notImplementedBulkClaimV2Handler() {
        return new NotImplementedBulkClaimV2Handler();
    }
}
