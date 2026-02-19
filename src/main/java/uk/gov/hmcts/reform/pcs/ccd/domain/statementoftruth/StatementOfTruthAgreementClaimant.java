package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum StatementOfTruthAgreementClaimant implements HasLabel {

    BELIEVE_TRUE("I believe that the facts stated in this claim form are true.");

    private final String label;

    StatementOfTruthAgreementClaimant(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}

