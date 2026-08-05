package uk.gov.hmcts.reform.pcs.ccd.domain.hearing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum ManageHearingOption implements HasLabel {

    ADD("Add a hearing"),
    EDIT("Edit a hearing"),
    CANCEL("Cancel a hearing");

    private final String label;
}
