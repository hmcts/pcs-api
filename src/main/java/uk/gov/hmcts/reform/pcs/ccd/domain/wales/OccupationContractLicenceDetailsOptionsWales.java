package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import uk.gov.hmcts.ccd.sdk.api.HasLabel;

public enum OccupationContractLicenceDetailsOptionsWales implements HasLabel {

    SECURE("Secure"),
    STANDARD("Standard"),
    OTHER("Other");

    private final String label;

    OccupationContractLicenceDetailsOptionsWales(String label) {
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }
}
