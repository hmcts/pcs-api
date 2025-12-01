package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.fees.client.model.Fee2Dto;
import uk.gov.hmcts.reform.fees.client.model.FeeLookupResponseDto;

import java.math.BigDecimal;

@Service
public class PCSFeesClient {

    private final FeesApi feesApi;

    public PCSFeesClient(FeesApi feesApi) {
        this.feesApi = feesApi;
    }

    public FeeLookupResponseDto lookupFee(ServiceName serviceName, Jurisdictions jurisdictions, String channel,
                                          String event, BigDecimal amount) {
        return this.feesApi.lookupFee(
            serviceName.service(),
            jurisdictions.jurisdiction1(),
            jurisdictions.jurisdiction2(),
            channel,
            event,
            null,
            amount,
            null
        );
    }

    public FeeLookupResponseDto lookupFee(ServiceName serviceName, Jurisdictions jurisdictions, String channel, String event,
                                          BigDecimal amount, String keyword) {
        return this.feesApi.lookupFee(
            serviceName.service(),
            jurisdictions.jurisdiction1(),
            jurisdictions.jurisdiction2(),
            channel,
            event,
            null,
            amount,
            keyword
        );
    }

    public Fee2Dto[] findRangeGroup(Jurisdictions jurisdictions, String channel, String event) {
        return this.feesApi.findRangeGroup(service, jurisdictions.jurisdiction1(), jurisdictions.jurisdiction2(),
                                           channel, event);
    }

}
