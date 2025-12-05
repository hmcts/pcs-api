package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fees.client.FeesApi;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@SuppressWarnings("DefaultAnnotationParam")
public class ClaimFeesClientContext implements FeesClientContext {

    private final FeesApi feesApi;
    private final Jurisdictions jurisdictions;
    private final ServiceName serviceName;

    public ClaimFeesClientContext(FeesApi feesApi,
        @Qualifier("feesJurisdictions") Jurisdictions jurisdictions,
        @Qualifier("feesServiceName") ServiceName serviceName) {
        this.feesApi = feesApi;
        this.jurisdictions = jurisdictions;
        this.serviceName = serviceName;
    }

    @Override
    public boolean supports(String feeTypesCode) {
        return true;
    }

    @Override
    public FeesApi getApi() {
        return feesApi;
    }

    @Override
    public Jurisdictions getJurisdictions() {
        return jurisdictions;
    }

    @Override
    public ServiceName getServiceName() {
        return serviceName;
    }

}
