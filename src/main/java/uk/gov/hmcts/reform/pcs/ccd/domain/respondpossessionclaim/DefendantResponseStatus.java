package uk.gov.hmcts.reform.pcs.ccd.domain.respondpossessionclaim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.ccd.sdk.api.HasLabel;

@AllArgsConstructor
@Getter
public enum DefendantResponseStatus implements HasLabel {

    CREATED("Created"),
    SUBMITTED("Submitted");

    private final String label;
}
