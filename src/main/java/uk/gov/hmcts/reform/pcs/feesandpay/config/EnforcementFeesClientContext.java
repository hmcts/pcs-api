package uk.gov.hmcts.reform.pcs.feesandpay.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

@Component
@Order(1)
public class EnforcementFeesClientContext implements FeesClientContext {

    private final EnforcementFeesApi enforcementFeesApi;
    private final Jurisdictions jurisdictions;
    private final ServiceName serviceName;

    public EnforcementFeesClientContext(EnforcementFeesApi enforcementFeesApi,
        @Qualifier("enforcementJurisdictions") Jurisdictions jurisdictions,
        @Qualifier("enforcementServiceName") ServiceName serviceName) {
        this.enforcementFeesApi = enforcementFeesApi;
        this.jurisdictions = jurisdictions;
        this.serviceName = serviceName;
    }

    @Override
    public boolean supports(FeeTypes feeTypes) {
        return FeeTypes.ENFORCEMENT_WARRANT_FEE == feeTypes || FeeTypes.ENFORCEMENT_WRIT_FEE == feeTypes;
    }

    @Override
    public FeesApi getApi() {
        return enforcementFeesApi;
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
