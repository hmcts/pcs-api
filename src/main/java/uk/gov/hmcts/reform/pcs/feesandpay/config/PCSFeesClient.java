package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PCSFeesClient {

    private final List<FeesClientContext> strategies;

    public PCSFeesClient(List<FeesClientContext> strategies) {
        this.strategies = strategies;
    }

    public FeeLookupResponseDto lookupFee(FeeTypes feeTypes, String channel, String event, BigDecimal amount,
                                          String keyword) {
        FeesClientContext context = strategies.stream()
            .filter(strategy -> strategy.supports(feeTypes))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No strategy found for fee type: " + feeTypes));

        return context.getApi().lookupFee(
            context.getServiceName().service(),
            context.getJurisdictions().jurisdiction1(),
            context.getJurisdictions().jurisdiction2(),
            channel,
            event,
            null,
            amount,
            keyword
        );
    }

}
