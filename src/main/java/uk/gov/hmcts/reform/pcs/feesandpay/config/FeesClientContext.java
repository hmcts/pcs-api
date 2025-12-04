package uk.gov.hmcts.reform.pcs.feesandpay.config;

import uk.gov.hmcts.reform.fees.client.FeesApi;

public interface FeesClientContext {

    boolean supports(String feeTypesCode);

    FeesApi getApi();

    Jurisdictions getJurisdictions();

    ServiceName getServiceName();

}
