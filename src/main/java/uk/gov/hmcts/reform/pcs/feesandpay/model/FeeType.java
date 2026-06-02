package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Getter;

@Getter
public enum FeeType {
    CASE_ISSUE_FEE("caseIssueFee"),
    HEARING_FEE("hearingFee"),
    ENFORCEMENT_WARRANT_FEE("enforcementWarrantFee"),
    ENFORCEMENT_WRIT_FEE("enforcementWritFee"),
    GEN_APP_STANDARD_FEE("genAppStandardFee"),
    GEN_APP_MAX_FEE("genAppMaxFee");

    private final String code;

    FeeType(String code) {
        this.code = code;
    }

}

