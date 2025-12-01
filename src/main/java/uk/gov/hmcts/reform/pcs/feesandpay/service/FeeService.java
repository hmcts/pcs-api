package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.feesandpay.config.Jurisdictions;
import uk.gov.hmcts.reform.pcs.feesandpay.config.ServiceName;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

public interface FeeService {

    FeeDetails getFee(ServiceName serviceName, Jurisdictions jurisdictions, String feeTypeKey);

}
