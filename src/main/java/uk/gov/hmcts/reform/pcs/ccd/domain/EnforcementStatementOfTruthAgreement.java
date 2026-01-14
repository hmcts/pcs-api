package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum EnforcementStatementOfTruthAgreement implements HasLabel {

    CERTIFY("I certify that:");

    private final String label;

    EnforcementStatementOfTruthAgreement(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}

