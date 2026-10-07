package uk.gov.hmcts.reform.pcs.ccd.domain.statementoftruth;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum AgreementDefendantLegalRep implements HasLabel {

    AGREED("The defendant believes that the facts stated in this claim form are true. "
        + "I am authorised by the defendant to sign this statement.");

    private final String label;

    AgreementDefendantLegalRep(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

}

