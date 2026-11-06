package uk.gov.hmcts.reform.pcs.ccd.domain.citizenclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum CitizenClaimantKind implements HasLabel {

    INDIVIDUAL("An individual"),
    ORGANISATION("A registered company");

    private final String label;
}
