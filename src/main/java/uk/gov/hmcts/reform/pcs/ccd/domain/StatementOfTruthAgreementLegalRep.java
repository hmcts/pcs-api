package uk.gov.hmcts.reform.pcs.ccd.domain;

import uk.gov.hmcts.ccd.sdk.api.CCD;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum StatementOfTruthAgreementLegalRep implements HasLabel {

    @CCD(label = "The claimant believes that the facts stated in this claim form are true. "
        + "I am authorised by the claimant to sign this statement.")
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

