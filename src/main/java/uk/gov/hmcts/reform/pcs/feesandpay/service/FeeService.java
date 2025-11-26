package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;

public interface FeeService {

    FeeDetails getFee(String feeTypeKey);

}
