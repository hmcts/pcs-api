package uk.gov.hmcts.reform.pcs.ccd.domain.caseworker;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum PartyType implements HasLabel {

    CLAIMANT("Claimant"),
    DEFENDANT("Defendant"),
    LITIGATION_FRIEND("Litigation friend");

    private final String label;
}