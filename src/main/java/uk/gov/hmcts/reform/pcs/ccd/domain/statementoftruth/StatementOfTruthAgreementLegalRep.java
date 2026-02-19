package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum StatementOfTruthAgreementLegalRep implements HasLabel {

    AGREED("The claimant believes that the facts stated in this claim form are true. "
        + "I am authorised by the claimant to sign this statement.");

    private final String label;

    StatementOfTruthAgreementLegalRep(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}

