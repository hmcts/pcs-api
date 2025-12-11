package uk.gov.hmcts.reform.pcs.feesandpay.service;

import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeDetails;
import uk.gov.hmcts.reform.pcs.feesandpay.model.FeeType;

public interface FeeService {

    FeeDetails getFee(FeeType feeType);

}
