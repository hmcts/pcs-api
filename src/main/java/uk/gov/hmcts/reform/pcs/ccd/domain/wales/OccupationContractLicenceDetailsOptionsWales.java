package uk.gov.hmcts.reform.pcs.ccd.domain.wales;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum OccupationContractLicenceDetailsOptionsWales implements HasLabel {

    SECURE("Secure"),
    STANDARD("Standard"),
    OTHER("Other");

    private final String label;
}
