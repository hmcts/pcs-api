package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum StatementOfTruthAgreement implements HasLabel {

    CERTIFY("I certify that:");

    private final String label;

    StatementOfTruthAgreement(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}

