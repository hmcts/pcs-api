package uk.gov.hmcts.reform.pcs.ccd.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum PartyRole implements HasLabel {

    CLAIMANT("Claimant"),
    DEFENDANT("Defendant"),
    INTERESTED_PARTY("Interested Party");

    private final String label;
}
