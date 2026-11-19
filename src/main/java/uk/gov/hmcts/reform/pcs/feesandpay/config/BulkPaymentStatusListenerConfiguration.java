package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.pcs.feesandpay.service.BulkPaymentStatusListener;
import uk.gov.hmcts.reform.pcs.feesandpay.service.NoOpBulkPaymentStatusListener;

@Configuration
public class BulkPaymentStatusListenerConfiguration {

    @Bean
    @ConditionalOnMissingBean(BulkPaymentStatusListener.class)
    public BulkPaymentStatusListener noOpBulkPaymentStatusListener() {
        return new NoOpBulkPaymentStatusListener();
    }

}
