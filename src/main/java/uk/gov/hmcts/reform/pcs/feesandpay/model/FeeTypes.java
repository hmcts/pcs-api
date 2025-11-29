package uk.gov.hmcts.reform.pcs.feesandpay.model;

import lombok.Getter;

@Getter
public enum FeeTypes {
    CASE_ISSUE_FEE("caseIssueFee"),
    HEARING_FEE("hearingFee"),
    ENFORCEMENT_WARRANT_FEE("enforcementWarrantFee"),
    ENFORCEMENT_WRIT_FEE("enforcementWritFee");

    private final String code;

    FeeTypes(String code) {
        this.code = code;
    }
}

