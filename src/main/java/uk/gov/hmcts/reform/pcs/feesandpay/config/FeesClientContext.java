package uk.gov.hmcts.reform.pcs.feesandpay.config;

import uk.gov.hmcts.reform.fees.client.FeesApi;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeTypes;

public interface FeesClientContext {

    boolean supports(FeeTypes feeTypes);

    FeesApi getApi();

    Jurisdictions getJurisdictions();

    ServiceName getServiceName();

}
